package com.tianji.remark.service;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author ke
 * @since 2025-03-25
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    void addLikeTimes(LikeRecordFormDTO likeRecordDTO);

    Set<Long> selectLikeStatus(List<Long> bizIds);

    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);

}
