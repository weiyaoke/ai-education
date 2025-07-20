package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-15
 */
@Api(tags = "我的课表相关接口")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {

    private final ILearningLessonService lessonService;

    @ApiOperation("查询我的课表，排序字段 latest_learn_time:学习时间排序，create_time:购买时间排序")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        return lessonService.queryMyLessons(query);
    }

    @GetMapping("/now")
    @ApiOperation("查询我正在学习的课程")
    public LearningLessonVO queryMyCurrentLesson() {
        return lessonService.queryMyCurrentLesson();
    }

    /**
     * 删除课程
     * @param courseIds
     */
    @DeleteMapping("/{courseIds}")
    @ApiOperation("删除课程")
    public void deleteCourseFromLesson(@ApiParam(value = "课程id" ,example = "1")
                                       @PathVariable("courseIds") List<Long> courseIds){
        Long userId = UserContext.getUser();
        this.lessonService.deleteCourseFromLesson(userId,courseIds);
    }

    /**
     * 校验当前用户是否可以学习当前课程
     * @param courseId 课程id
     * @return lessonId，如果是报名了则返回lessonId，否则返回空
     */
    @GetMapping("/{courseId}/valid")
    @ApiOperation("校验当前用户是否可以学习当前课程")
    Long isLessonValid(@ApiParam(value = "课程id" ,example = "1")
                         @PathVariable("courseId") Long courseId){
        return this.lessonService.isLessonValid(courseId);
    }
    @GetMapping("/{courseId}")
    @ApiOperation("查询指定课程信息")
    public LearningLessonVO queryLessonByCourseId(
            @ApiParam(value = "课程id" ,example = "1") @PathVariable("courseId") Long courseId) {
        return this.lessonService.queryLessonByCourseId(courseId);
    }
    /**
     * 统计课程学习人数
     * @param courseId 课程id
     * @return 学习人数
     */
    @GetMapping("/{courseId}/count")
    @ApiOperation("统计课程学习人数")
    Integer countLearningLessonByCourse( @ApiParam(value = "课程id" ,example = "1")
                                         @PathVariable("courseId") Long courseId){
        return this.lessonService.countLearningLessonByCourse(courseId);
    }

    @ApiOperation("创建学习计划")
    @PostMapping("/plans")
    public void createLearningPlans(@Valid  @RequestBody LearningPlanDTO planDTO){
        this.lessonService.createLearningPlan(planDTO.getCourseId(), planDTO.getFreq());
    }
    @ApiOperation("查询我的学习计划")
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlans(PageQuery query){
        return this.lessonService.queryMyPlans(query);
    }
}
