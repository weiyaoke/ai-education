package com.tianji.learning.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.po.TypePointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tianji.learning.constants.RedisConstants.POINTS_BOARD_KEY_PREFIX;
import static com.tianji.learning.constants.RedisConstants.POINTS_BOARD_RECORD_TABLE_PREFIX;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-26
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 根据每个赛季新增数据记录表
     * @param season
     */
    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        this.getBaseMapper().createPointsBoardTableBySeason(POINTS_BOARD_RECORD_TABLE_PREFIX + season);
    }

    /**
     * 添加学生积分记录
     * @param userId
     * @param points
     * @param pointsRecordType
     */
    @Override
    public void addSPointsRecord(Long userId, int points, PointsRecordType pointsRecordType) {
        //1、判断积分类型每日是否有上限
        LocalDateTime now = LocalDateTime.now();
        int realPoints = points;
        int maxPoints = pointsRecordType.getMaxPoints();
        if (maxPoints > 0){
            //如果是有积分上限的话
            //2、查询今日的积分,如果今日积分达标的话则直接返回即可，不需要后面的操作
            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
            int todayPoints = this.queryUserPointsByTypeAndDate(userId,pointsRecordType,begin,end);
            if (todayPoints >= maxPoints){
                return;
            }
            //3、将传入的需要添加的最新的积分和今日的积分相加比较是否超过今日的积分上限
            if(points + todayPoints > maxPoints){
                //4、将超过今日积分的部分去掉，把真正需要添加的积分计算出来
                realPoints = maxPoints - todayPoints;
            }
        }
        //5、保存到数据库
        PointsRecord pointsRecord = PointsRecord.builder()
                .userId(userId)
                .points(realPoints)
                .type(pointsRecordType)
                .build();
        this.save(pointsRecord);
        //6、累加积分数据到Redis的SortedSet中
        String key = POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        this.redisTemplate.opsForZSet().incrementScore(key,userId.toString(),realPoints);
    }

    /**
     * 查询今日积分
     * @return
     */
    @Override
    public List<PointsStatisticsVO> queryTodayPoints() {
        // 1.获取用户
        Long userId = UserContext.getUser();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        // 3.构建查询条件
        LambdaQueryWrapper<PointsRecord> wrapper = Wrappers.<PointsRecord>lambdaQuery()
                .eq(PointsRecord::getUserId, userId)
                .between(PointsRecord::getCreateTime, begin, end);
        // 4.查询
        List<TypePointsRecord> list = getBaseMapper().queryUserPointsByDate(wrapper);
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 5.封装返回
        List<PointsStatisticsVO> vos = new ArrayList<>(list.size());
        for (TypePointsRecord p : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }

    private int queryUserPointsByTypeAndDate(
            Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        // 1.查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(type != null, PointsRecord::getType, type)
                .between(begin != null && end != null, PointsRecord::getCreateTime, begin, end);
        // 2.调用mapper，查询结果
        Integer points = getBaseMapper().queryUserPointsByTypeAndDate(wrapper);
        // 3.判断并返回
        return points == null ? 0 : points;
    }
}
