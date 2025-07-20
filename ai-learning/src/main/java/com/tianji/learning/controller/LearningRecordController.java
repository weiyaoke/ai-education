package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-17
 */
@RestController
@RequestMapping("/learning-records")
@Api(tags = "学习进度记录")
@RequiredArgsConstructor
public class LearningRecordController {
    private final ILearningRecordService iLearningRecordService;
    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @ApiOperation("查询当前用户指定课程的学习进度")
    @GetMapping("/course/{courseId}")
    LearningLessonDTO queryLearningRecordByCourse(@ApiParam(name = "courseId",example = "1")
                                                  @PathVariable("courseId") Long courseId){
        return this.iLearningRecordService.queryLearningRecordByCourse(courseId);
    }
    @ApiOperation("提交学习记录")
    @PostMapping
    public void addLearningRecord(@RequestBody LearningRecordFormDTO formDTO){
        this.iLearningRecordService.addLearningRecord(formDTO);
    }
}
