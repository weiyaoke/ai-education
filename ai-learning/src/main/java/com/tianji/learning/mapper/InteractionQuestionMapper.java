package com.tianji.learning.mapper;

import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 互动提问的问题表 Mapper 接口
 * </p>
 *
 * @author ke
 * @since 2025-03-21
 */
public interface InteractionQuestionMapper extends BaseMapper<InteractionQuestion> {

    @Select("select * from interaction_question where id = #{id}")
    InteractionQuestion getById(Long id);

}
