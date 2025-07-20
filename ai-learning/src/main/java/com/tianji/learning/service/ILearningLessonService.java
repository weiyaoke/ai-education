package com.tianji.learning.service;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author ke
 * @since 2025-03-15
 */
public interface ILearningLessonService extends IService<LearningLesson> {


    void addUserLessons(Long userId, List<Long> courseIds);

    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    LearningLessonVO queryMyCurrentLesson();

    void deleteCourseFromLesson(Long userId, List<Long> courseId);


    Long isLessonValid(Long courseId);

    LearningLessonVO queryLessonByCourseId(Long courseId);

    Integer countLearningLessonByCourse(Long courseId);

    void createLearningPlan(Long courseId, Integer freq);

    LearningLesson getLessonByCourseId(Long userId, Long courseId);

    LearningPlanPageVO queryMyPlans(PageQuery query);

}

