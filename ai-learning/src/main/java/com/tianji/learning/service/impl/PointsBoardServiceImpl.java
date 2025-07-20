package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.TableInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;;

import static com.tianji.learning.constants.RedisConstants.POINTS_BOARD_KEY_PREFIX;
import static com.tianji.learning.constants.RedisConstants.POINTS_BOARD_TABLE_PREFIX;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-26
 */
@RequiredArgsConstructor
@Service
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;

    /**
     * 创建表
     * @param season
     */
    @Override
    public void createPointsBoardBySeason(Integer season) {
        this.getBaseMapper().createPointsBoardBySeason(POINTS_BOARD_TABLE_PREFIX + season);
    }

    /**
     * 分页查询指定赛季的积分排行榜
     * @param pointsBoardQuery
     * @return
     */
    @Override
    public PointsBoardVO querySeasonPointsBoard(PointsBoardQuery pointsBoardQuery) {
        //0.0、获取当前用户
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        //1、根据season来判断查询的是当前赛季排行榜还是历史排行榜
        Boolean isNowSeason = pointsBoardQuery.getSeason() == null || pointsBoardQuery.getSeason() == 0;
        //2、查询个人的排行信息
        // 2.1、如果是当前赛季的话则在redis中查询数据
        //2.2、如果是查询历史赛季的话则在数据库中查询
        String key = POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        PointsBoard pointsBoard = null;
        pointsBoard = isNowSeason ? this.queryPersonSeasonInfoFromRedis(key,userId) : this.queryPersonSeasonInfoFromSql(userId, pointsBoardQuery.getSeason());
        //3、查询所有用户的排行信息
        List<PointsBoard> list = null;
        list = isNowSeason ? this.queryUserSeasonInfoFromRedis(key,pointsBoardQuery.getPageNo(),pointsBoardQuery.getPageSize()) : this.queryUserSeasonInfoFromSql(pointsBoardQuery);
        //4、封装vo
        PointsBoardVO pointsBoardVO = new PointsBoardVO();
        if (ObjectUtil.isNotEmpty(pointsBoard)){
            pointsBoardVO.setRank(pointsBoard.getRank());
            pointsBoardVO.setPoints(pointsBoardVO.getPoints());
        }
        //4.1、需要根据用户id获取用户名
        if (ObjectUtil.isEmpty(list)){
            return pointsBoardVO;
        }
        //4.2填充前100名上榜人信息
        Set<Long> userIds = CollStreamUtil.toSet(list, PointsBoard::getUserId);
        List<UserDTO> userDTOS = null;
        if (ObjectUtil.isNotEmpty(userIds)){
            userDTOS = this.userClient.queryUserByIds(userIds);
        }
        if (ObjectUtil.isEmpty(userDTOS)){
            return null;
        }
        Map<Long, UserDTO> userDTOMap = CollStreamUtil.toMap(userDTOS, UserDTO::getId, u -> u);

        //4.2、把userId换成userName
        List<PointsBoardItemVO> pointsBoardItemVOS = StreamUtil.of(list).map(r -> {
            PointsBoardItemVO vo = new PointsBoardItemVO();
            UserDTO userDTO = userDTOMap.get(r.getUserId());
            vo.setName(userDTO.getName());
            vo.setRank(r.getRank());
            vo.setPoints(r.getPoints());
            return vo;
        }).collect(Collectors.toList());
        //5、返回
        pointsBoardVO.setBoardList(pointsBoardItemVOS);
        return pointsBoardVO;
    }
    private PointsBoard queryPersonSeasonInfoFromRedis(String key,Long userId) {
        //从redis中获取个人的积分排行信息
        Double points = this.redisTemplate.opsForZSet().score(key, userId.toString());
        Long rank = this.redisTemplate.opsForZSet().reverseRank(key, userId.toString());
        PointsBoard p = new PointsBoard();
        p.setPoints(points == null ? 0 : points.intValue());
        p.setRank(rank == null ? 0 : rank.intValue() + 1);
        return p;
    }
    private PointsBoard queryPersonSeasonInfoFromSql(Long userId, Long season) {
        //根据不同的赛季获得用户的积分
        //1、设置数据表名
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + season);
        //2、构造查询条件
        LambdaQueryWrapper<PointsBoard> wrapper = Wrappers.<PointsBoard>lambdaQuery()
                .eq(PointsBoard::getUserId, userId);
        PointsBoard pointsBoard = this.getOne(wrapper);
        if (ObjectUtil.isEmpty(pointsBoard)){
            return null;
        }
        pointsBoard.setRank(pointsBoard.getId().intValue());
        //3、及时清楚
        TableInfoContext.remove();
        return pointsBoard;
    }
    @Override
    public List<PointsBoard> queryUserSeasonInfoFromRedis(String key, Integer pageNo,Integer pageSize) {
        //1、从redis中获取所有用户的积分信息
        //2、在redis中从第几条开始查询
        int from = (pageNo - 1) * pageSize;
        int to = from + pageSize - 1;
        int rank = from + 1;
        Set<ZSetOperations.TypedTuple<String>> tuples = this.redisTemplate.opsForZSet().reverseRangeWithScores(key, from, to);
        if (ObjectUtil.isEmpty(tuples)){
            return ListUtil.empty();
        }
        List<PointsBoard> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples){
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            if (userId == null || points == null) {
                continue;
            }
            PointsBoard p = new PointsBoard();
            p.setUserId(Long.valueOf(userId));
            p.setPoints(points.intValue());
            p.setRank(rank++);
            list.add(p);
        }
        return list;
    }
    private List<PointsBoard> queryUserSeasonInfoFromSql(PointsBoardQuery pointsBoardQuery) {
        //1、设置数据表名
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + pointsBoardQuery.getSeason());
        //2、分页查询
        Page<PointsBoard> iPage = new Page<>(pointsBoardQuery.getPageNo(), pointsBoardQuery.getPageSize());
        Page<PointsBoard> pointsBoardPage = this.page(iPage);
        if (ObjectUtil.isEmpty(pointsBoardPage)){
            return ListUtil.empty();
        }
        List<PointsBoard> records = pointsBoardPage.getRecords();
        //3、处理一下数据
        List<PointsBoard> res = StreamUtil.of(records).peek(r -> {
            r.setRank(r.getId().intValue());
        }).collect(Collectors.toList());
        return res;
    }
}
