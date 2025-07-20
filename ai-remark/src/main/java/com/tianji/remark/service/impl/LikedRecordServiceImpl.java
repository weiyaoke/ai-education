package com.tianji.remark.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.text.StrFormatter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-25
 */
@RequiredArgsConstructor
//@Service
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
    private final RabbitMqHelper rabbitMqHelper;
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
        //2、统计点赞数
        LambdaQueryWrapper<LikedRecord> queryWrapper = Wrappers.<LikedRecord>lambdaQuery()
                .eq(LikedRecord::getBizId, likeRecordDTO.getBizId());
        int countLikeTimes = this.count(queryWrapper);
        LikedTimesDTO likedTimesDTO = LikedTimesDTO.builder()
                .bizId(likeRecordDTO.getBizId())
                .likedTimes(countLikeTimes)
                .build();
        //3、通知MQ业务方
        rabbitMqHelper.send(LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE,likeRecordDTO.getBizType()),
                likedTimesDTO);
    }

    /**
     * 批量查询点赞状态
     * @param bizIds
     */
    @Override
    public Set<Long> selectLikeStatus(List<Long> bizIds) {
        LambdaQueryWrapper<LikedRecord> queryWrapper = Wrappers.<LikedRecord>lambdaQuery()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .in(LikedRecord::getBizId, bizIds);
        List<LikedRecord> likedRecords = this.list(queryWrapper);
        return CollStreamUtil.toSet(likedRecords, LikedRecord::getBizId);
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {

    }

    private Boolean unlike(LikeRecordFormDTO likeRecordDTO) {
        //1、直接删除点赞记录，它会直接返回更新的影响行数，如果是0的话说明没有点过赞这样更新失败，如果是大于0的话说明更新成功
        LambdaQueryWrapper<LikedRecord> queryWrapper = Wrappers.<LikedRecord>lambdaQuery()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, likeRecordDTO.getBizId());
        return this.remove(queryWrapper);
    }

    private Boolean like(LikeRecordFormDTO likeRecordDTO) {
        //1、幂等性判断  查看是否已经存在点赞记录
        LambdaQueryWrapper<LikedRecord> queryWrapper = Wrappers.<LikedRecord>lambdaQuery()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, likeRecordDTO.getBizId());
        int countLikeTimes = this.count(queryWrapper);
        if (countLikeTimes > 0){
            return false;
        }
        //2、新增点赞记录
        LikedRecord likedRecord = BeanUtils.copyBean(likeRecordDTO, LikedRecord.class);
        likedRecord.setUserId(UserContext.getUser());
        this.save(likedRecord);
        return true;
    }
}
