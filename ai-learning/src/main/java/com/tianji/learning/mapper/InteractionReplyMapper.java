package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.InteractionReply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 互动问题的回答或评论 Mapper 接口
 * </p>
 *
 * @author ke
 * @since 2025-03-21
 */
public interface InteractionReplyMapper extends BaseMapper<InteractionReply> {

    @Select("select * from interation_reply where answer_id = #{answerId}")
    InteractionReply getByAnswerId(Long answerId);

}
