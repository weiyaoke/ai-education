package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 学生课程表 Mapper 接口
 * </p>
 *
 * @author ke
 * @since 2025-03-15
 */
public interface LearningLessonMapper extends BaseMapper<LearningLesson> {
    @Select("select SUM(week_freq) from learning_lesson where user_id = #{userId} and status in (0,1) and plan_status = 1")
    Integer selectWeekPlanLearningSessionNum(@Param("userId") Long userId);
}
