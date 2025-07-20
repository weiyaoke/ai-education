package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-15
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final LearningRecordMapper learningRecordMapper;
    private final LearningLessonMapper learningLessonMapper;
    /**
     * 添加课程
     * @param userId
     * @param courseIds
     */
    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        //根据courseIds直接去查询课程的信息即可，其中包括课程的id，有效期
        List<CourseSimpleInfoDTO> courseSimpleInfoDTOS = this.courseClient.getSimpleInfoList(courseIds);
        if (ObjectUtil.isEmpty(courseSimpleInfoDTOS)){
            log.error("课程信息不存在，无法添加到列表");
            return;
        }
        List<LearningLesson> learningLessons = StreamUtil.of(courseSimpleInfoDTOS).map(
                cInfo -> {
                    LearningLesson learningLesson = new LearningLesson();
                    learningLesson.setUserId(userId);
                    learningLesson.setCourseId(cInfo.getId());
                    if (ObjectUtil.isNotEmpty(cInfo.getValidDuration()) && cInfo.getValidDuration() > 0) {
                        learningLesson.setCreateTime(LocalDateTime.now());
                        learningLesson.setExpireTime(LocalDateTime.now().plusMonths(cInfo.getValidDuration()));
                    }
                    return learningLesson;
                }
        ).collect(Collectors.toList());
        //插入数据库咯
        this.saveBatch(learningLessons);
    }

    /**
     * 查询我的课表
     * @param query
     * @return
     */
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //1、先获取用户id
        Long userId = UserContext.getUser();
        //2、根据用户id查询用户的课表
        //2.1 构建分页条件
        Page<LearningLesson> iPage = new Page<>(query.getPageNo(),query.getPageSize());
        //2.2 构建查询条件
        LambdaQueryWrapper<LearningLesson> queryWrapper = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .orderByDesc(LearningLesson::getLatestLearnTime);
        //2.3 执行查询
        Page<LearningLesson> page = this.page(iPage, queryWrapper);
        List<LearningLesson> records = page.getRecords();
        //3、查询课程信息
        Set<Long> courseIds = CollStreamUtil.toSet(records, LearningLesson::getCourseId);
        List<CourseSimpleInfoDTO> courseSimpleInfoDTOS = this.courseClient.getSimpleInfoList(courseIds);
        if (ObjectUtil.isEmpty(courseSimpleInfoDTOS)){
            log.error("课程信息不存在，无法添加到列表");
            return PageDTO.empty(page);
        }
        Map<Long, CourseSimpleInfoDTO> lessonsMap = CollStreamUtil.toMap(courseSimpleInfoDTOS, CourseSimpleInfoDTO::getId, c -> c);
        //4、组装数据
        List<LearningLessonVO> learningLessonVOS = StreamUtil.of(records).map(r -> {
            LearningLessonVO learningLessonVO = BeanUtils.copyBean(r, LearningLessonVO.class);
            CourseSimpleInfoDTO courseSimpleInfoDTO = lessonsMap.get(r.getCourseId());
            learningLessonVO.setCourseName(courseSimpleInfoDTO.getName());
            learningLessonVO.setCourseCoverUrl(courseSimpleInfoDTO.getCoverUrl());
            learningLessonVO.setSections(courseSimpleInfoDTO.getSectionNum());
            return learningLessonVO;
        }).collect(Collectors.toList());
        return PageDTO.of(page,learningLessonVOS);
    }

    /**
     * 查询我正在学习的课程
     * @return
     */
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        //1、获取用户的userId
        Long userId = UserContext.getUser();
        //2、根据userId查询我的课表中最近学习的课程
        LambdaQueryWrapper<LearningLesson> queryWrapperLearningLesson = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime);
        List<LearningLesson> learningLessonList = this.list(queryWrapperLearningLesson);
        if (ObjectUtil.isEmpty(learningLessonList)){
            return null;
        }
        LearningLesson nowLearningLesson = learningLessonList.get(0);
        //3、根据courseId查询课程信息
        CourseFullInfoDTO courseInfoById = this.courseClient.getCourseInfoById(nowLearningLesson.getCourseId(), false, false);
        if (ObjectUtil.isEmpty(courseInfoById)){
            return null;
        }
        //4、根据我的课表中的课程信息查询最近一次学习的小节序号和名称
        CataSimpleInfoDTO cataSimpleInfoDTO = this.catalogueClient.batchQueryCatalogue(ListUtil.of(nowLearningLesson.getLatestSectionId())).get(0);
        //5、统计总报名课程数
        LambdaQueryWrapper<LearningLesson> queryWrapperLearningLessonNum = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getUserId, userId);
        List<LearningLesson> learningLessonListNum = this.list(queryWrapperLearningLessonNum);
        //6、组装
        LearningLessonVO learningLessonVO = BeanUtils.copyBean(nowLearningLesson, LearningLessonVO.class);
        learningLessonVO.setCourseName(courseInfoById.getName());
        learningLessonVO.setCourseCoverUrl(courseInfoById.getCoverUrl());
        learningLessonVO.setLatestSectionIndex(cataSimpleInfoDTO.getCIndex());
        learningLessonVO.setLatestSectionName(cataSimpleInfoDTO.getName());
        learningLessonVO.setSections(courseInfoById.getSectionNum());
        learningLessonVO.setCourseAmount(learningLessonListNum.size());
        return learningLessonVO;
    }

    /**
     * 删除失效课程
     * @param userId
     * @param courseIds
     */
    @Override
    public void deleteCourseFromLesson(Long userId, List<Long> courseIds) {
        //先在课程表中查询出来
        if (ObjectUtil.isEmpty(userId)){
            userId = UserContext.getUser();
        }
        LambdaQueryWrapper<LearningLesson> queryWrapper = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds);
        this.remove(queryWrapper);
    }

    /**
     * 校验用户是否有权限播放视频
     * @param courseId
     * @return
     */
    @Override
    public Long isLessonValid(Long courseId) {
        //先获取登录用户
        Long userId = UserContext.getUser();
        if (ObjectUtil.isEmpty(userId)){
            return null;
        }
        LambdaQueryWrapper<LearningLesson> queryWrapper = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId,userId);
        List<LearningLesson> learningLessonList = this.list(queryWrapper);
        if (ObjectUtil.isEmpty(learningLessonList)){
            return null;
        }
        return learningLessonList.get(0).getId();
    }

    /**
     * 根据课程id获取用户的课表信息
     * @param courseId
     */
    @Override
    public LearningLesson getLessonByCourseId(Long userId,Long courseId) {
        LambdaQueryWrapper<LearningLesson> queryWrapper = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId,userId);
        List<LearningLesson> learningLessonList = this.list(queryWrapper);
        if (ObjectUtil.isEmpty(learningLessonList)){
            log.error("该用户没有这个课程");
            return null;
        }
        return learningLessonList.get(0);
    }

    /**
     * 查询指定课程信息
     * @param courseId
     * @return
     */
    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {
        //查询用户的课表是否存在这个课程
        Long userId = UserContext.getUser();
        LambdaQueryWrapper<LearningLesson> queryWrapper = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId,userId);
        List<LearningLesson> learningLessonList = this.list(queryWrapper);
        if (ObjectUtil.isEmpty(learningLessonList)){
            log.error("用户课程表没有这个课程");
            return null;
        }
        //转VO
        LearningLessonVO learningLessonVO = BeanUtils.copyBean(learningLessonList.get(0), LearningLessonVO.class);
        return learningLessonVO;
    }

    /**
     * 统计课程学习人数
     * @param courseId
     * @return
     */
    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        //查询用户的课表是否存在这个课程
        LambdaQueryWrapper<LearningLesson> queryWrapper = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .in(LearningLesson::getStatus, LessonStatus.LEARNING, LessonStatus.FINISHED, LessonStatus.NOT_BEGIN);;
        List<LearningLesson> learningLessonList = this.list(queryWrapper);
        return learningLessonList.size();
    }

    /**
     * 查询我的计划
     * @param query
     * @return
     */
    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        LearningPlanPageVO learningPlanPageVO = new LearningPlanPageVO();
        Long userId = UserContext.getUser();
        LocalDate localDate = LocalDate.now();
        LocalDateTime weekBeginTime = DateUtils.getWeekBeginTime(localDate);
        LocalDateTime weekEndTime = DateUtils.getWeekEndTime(localDate);
        //1、查询本周实际学习小节数量 调用课程记录表查询
        LambdaQueryWrapper<LearningRecord> queryWrapperActualLessonRecord = new LambdaQueryWrapper<>();
        queryWrapperActualLessonRecord.eq(LearningRecord::getUserId,userId);
        queryWrapperActualLessonRecord.eq(LearningRecord::getFinished, LessonStatus.LEARNING);
        queryWrapperActualLessonRecord.gt(LearningRecord::getFinishTime,weekBeginTime).lt(LearningRecord::getFinishTime,weekEndTime);
        Integer weekSessionActualNum = this.learningRecordMapper.selectCount(queryWrapperActualLessonRecord);
        //2、查询本周计划学习小节数量 调用课程表查询
        Integer weekSessionPlanNum = this.learningLessonMapper.selectWeekPlanLearningSessionNum(userId);
        learningPlanPageVO.setWeekFinished(weekSessionActualNum);
        learningPlanPageVO.setWeekTotalPlan(weekSessionPlanNum);
        //3、TODO 本周学习积分
        //4、分页查询具体课程计划信息
        Page<LearningLesson> iPage = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<LearningLesson> queryWrapperLessonPage = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus,PlanStatus.PLAN_RUNNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING);
        Page<LearningLesson> lessonPage = this.page(iPage, queryWrapperLessonPage);
        List<LearningLesson> learningLessons = lessonPage.getRecords();
        learningPlanPageVO.setTotal(lessonPage.getTotal());
        learningPlanPageVO.setPages(lessonPage.getPages());
        if (ObjectUtil.isEmpty(learningLessons)){
            return learningPlanPageVO;
        }
        Set<Long> courseIds = StreamUtil.of(learningLessons).map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> simpleInfoList = this.courseClient.getSimpleInfoList(courseIds);
        Map<Long, CourseSimpleInfoDTO> map = CollStreamUtil.toMap(simpleInfoList, CourseSimpleInfoDTO::getId, c -> c);
        List<LearningPlanVO> collect = StreamUtil.of(learningLessons).map(l -> {
            LearningPlanVO learningPlanVO = BeanUtils.copyProperties(l, LearningPlanVO.class);
            learningPlanVO.setCourseName(map.get(l.getCourseId()).getName());
            learningPlanVO.setSections(map.get(l.getCourseId()).getSectionNum());
            //查询该课程本周已经学习完的章节数
            Integer learnedSessionNum = selectLessonSessionLearned(l.getId());
            learningPlanVO.setWeekLearnedSections(learnedSessionNum);
            return learningPlanVO;
        }).collect(Collectors.toList());
        learningPlanPageVO.setList(collect);
        return learningPlanPageVO;
    }

    private Integer selectLessonSessionLearned(Long id) {
        LambdaQueryWrapper<LearningRecord> learningRecordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        learningRecordLambdaQueryWrapper.eq(LearningRecord::getLessonId,id);
        learningRecordLambdaQueryWrapper.eq(LearningRecord::getFinished,LessonStatus.LEARNING);
        learningRecordLambdaQueryWrapper.eq(LearningRecord::getUserId,UserContext.getUser());
        return this.learningRecordMapper.selectCount(learningRecordLambdaQueryWrapper);
    }

    /**
     * 新增用户学习计划
     * @param courseId
     * @param weekFreq
     */
    @Override
    public void createLearningPlan(Long courseId, Integer weekFreq) {
        //1、先获取用户id
        Long userId = UserContext.getUser();
        //2、查询出来用户的课表信息
        LambdaQueryWrapper<LearningLesson> queryWrapper = Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId);
        LearningLesson learningLesson = this.getOne(queryWrapper);
        if (ObjectUtil.isEmpty(learningLesson)){
            log.error("用户没有购买这个课程");
            return;
        }
        if(learningLesson.getPlanStatus() == PlanStatus.NO_PLAN) {
            learningLesson.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        //3、补充courseId和weekFreq信息
        learningLesson.setWeekFreq(weekFreq);
        //4、update更新
        this.updateById(learningLesson);
    }
}
