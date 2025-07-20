package com.tianji.remark.service.impl;

import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;
import static com.tianji.remark.contants.RedisConstants.LIKES_TIMES_KEY_PREFIX;
import static com.tianji.remark.contants.RedisConstants.LIKE_BIZ_KEY_PREFIX;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-25
 */
@RequiredArgsConstructor
@Service
public class LikedRecordRedisServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
    private final RabbitMqHelper rabbitMqHelper;
    private final StringRedisTemplate redisTemplate;
    /**
     * 新增或取消点赞
     * @param likeRecordDTO
     */
    @Override
    public void addLikeTimes(LikeRecordFormDTO likeRecordDTO) {
        //1、判断是否是点赞还是取消是否成功
        Boolean success = likeRecordDTO.getLiked() ? this.like(likeRecordDTO) : this.unlike(likeRecordDTO);
        if (!success){
            return;
        }
        String key = LIKE_BIZ_KEY_PREFIX + likeRecordDTO.getBizId();
        //2、统计点赞数
        Long nums = this.redisTemplate.opsForSet().size(key);
        if (ObjectUtil.isEmpty(nums)){
            return;
        }
        //3、缓存点赞总数
        this.redisTemplate.opsForZSet().add(LIKES_TIMES_KEY_PREFIX + likeRecordDTO.getBizType(),
                likeRecordDTO.getBizId().toString(),
                nums);
    }
    /**
     * 批量查询点赞状态
     * @param bizIds
     */
    @Override
    public Set<Long> selectLikeStatus(List<Long> bizIds) {
        Long userId = UserContext.getUser();
        List<Object> likeStatus = this.redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                StringRedisConnection src = (StringRedisConnection) connection;
                for (Long bizId : bizIds) {
                    String key = LIKE_BIZ_KEY_PREFIX + bizId;
                    src.sIsMember(key, userId.toString());
                }
                return null;
            }
        });
        //返回用户是否点赞的业务id
        Set<Long> result = IntStream.range(0, likeStatus.size()).filter(i -> (Boolean) likeStatus.get(i)).mapToObj(bizIds::get).collect(Collectors.toSet());
        return result;
    }
    /**
     * 定时从redis中查询点赞总数并发送消息
     * @param bizType
     * @param maxBizSize
     */
    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        //1、获取某一个类型的点赞数据
        Set<ZSetOperations.TypedTuple<String>> tuples = this.redisTemplate.opsForZSet().popMin(LIKES_TIMES_KEY_PREFIX + bizType, maxBizSize);
        //2、得到具体的业务和与之对应的点赞数
        if (ObjectUtil.isEmpty(tuples)){
            return;
        }
        List<LikedTimesDTO> list = StreamUtil.of(tuples).map(tuple -> {
            String bizId = tuple.getValue();
            Double likeTimes = tuple.getScore();
            LikedTimesDTO likedTimesDTO = LikedTimesDTO.builder().bizId(Long.valueOf(bizId)).likedTimes(likeTimes.intValue()).build();
            return likedTimesDTO;
        }).collect(Collectors.toList());
        //3、发送消息
        this.rabbitMqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, bizType),
                list);
    }
    private Boolean unlike(LikeRecordFormDTO likeRecordDTO) {
        //1、直接删除点赞记录，它会直接返回更新的影响行数，如果是0的话说明没有点过赞这样更新失败，如果是大于0的话说明更新成功
        String key = LIKE_BIZ_KEY_PREFIX + likeRecordDTO.getBizId();
        Long res = this.redisTemplate.opsForSet().remove(key, UserContext.getUser().toString());
        return res != null && res > 0;
    }
    private Boolean like(LikeRecordFormDTO likeRecordDTO) {
        //1、新增点赞记录
        String key = LIKE_BIZ_KEY_PREFIX + likeRecordDTO.getBizId();
        Long res = this.redisTemplate.opsForSet().add(key, UserContext.getUser().toString());
        return res != null && res > 0;
    }
}
