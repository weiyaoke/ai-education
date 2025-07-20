package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-21
 */
@RequiredArgsConstructor
@Service
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final UserClient userClient;
    private final IInteractionReplyService replyService;
    private final CatalogueClient catalogueClient;
    private final CategoryCache categoryCache;
    private final CourseClient courseClient;
    private final SearchClient searchClient;
    private final InteractionReplyMapper replyMapper;

    /**
     * 新增下互动问题
     * @param questionFormDTO
     */
    @Override
    public void saveInteractionQuestion(QuestionFormDTO questionFormDTO) {
        //1、获取用户的userId
        Long userId = UserContext.getUser();
        //2、转化为po
        InteractionQuestion interactionQuestion = BeanUtils.copyBean(questionFormDTO, InteractionQuestion.class);
        interactionQuestion.setUserId(userId);
        //3、保存到数据库
        this.save(interactionQuestion);
    }

    /**
     * 根据id查询具体的问题
     * @param id
     * @return
     */
    @Override
    public QuestionVO selectInteractionQuestionById(String id) {
        //1、查询具体问题
        InteractionQuestion interactionQuestion = this.getById(id);
        //1.1拷贝属性
        QuestionVO questionVO = BeanUtils.copyBean(interactionQuestion, QuestionVO.class);
        //2、判断问题是否存在还是问题是否违规被隐藏了
        if (ObjectUtil.isEmpty(interactionQuestion) || ObjectUtil.equal(interactionQuestion.getHidden(), Boolean.TRUE)) {
            throw new BadRequestException("问题似乎违规，无法查看！！！");
        }
        //3、判断问题是否是匿名的
        UserDTO userDTO = null;
        if (ObjectUtil.equal(interactionQuestion.getAnonymity(), Boolean.FALSE)) {
            userDTO = this.userClient.queryUserById(interactionQuestion.getUserId());
        }
        if (ObjectUtil.isNotEmpty(userDTO)) {
            questionVO.setUserName(userDTO.getName());
            questionVO.setUserIcon(questionVO.getUserIcon());
        }
        return questionVO;
    }

    /**
     * 管理端问题分页查询
     * @param questionAdminPageQuery
     * @return
     */
    @Override
    public PageDTO<QuestionAdminVO> interactionQuestionAdminPage(QuestionAdminPageQuery questionAdminPageQuery) {
        //0、校验参数
        List<Long> courseIds = null;
        if (ObjectUtil.isNotEmpty(questionAdminPageQuery.getCourseName())){
            //1、根据课程名称查询课程id
            courseIds = this.searchClient.queryCoursesIdByName(questionAdminPageQuery.getCourseName());
            if (ObjectUtil.isEmpty(courseIds)){
                return PageDTO.empty(0l,0l);
            }
        }
        //2、分页查询
        Page<InteractionQuestion> iPage = new Page<>(questionAdminPageQuery.getPageNo(), questionAdminPageQuery.getPageSize());
        LambdaQueryWrapper<InteractionQuestion> queryWrapperInteractionQuestion = Wrappers.<InteractionQuestion>lambdaQuery()
                .in(ObjectUtil.isNotEmpty(courseIds),InteractionQuestion::getCourseId, courseIds)
                .eq(ObjectUtil.isNotEmpty(questionAdminPageQuery.getStatus()), InteractionQuestion::getStatus, questionAdminPageQuery.getStatus())
                .gt(ObjectUtil.isNotEmpty(questionAdminPageQuery.getBeginTime()), InteractionQuestion::getCreateTime, questionAdminPageQuery.getBeginTime())
                .lt(ObjectUtil.isNotEmpty(questionAdminPageQuery.getEndTime()), InteractionQuestion::getCreateTime, questionAdminPageQuery.getEndTime())
                .orderByAsc(InteractionQuestion::getCreateTime);
        Page<InteractionQuestion> page = this.page(iPage, queryWrapperInteractionQuestion);
        List<InteractionQuestion> records = page.getRecords();
        if (ObjectUtil.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //3、根据课程id查询课程信息
        Set<Long> cIds = CollStreamUtil.toSet(records, InteractionQuestion::getCourseId);
        List<CourseSimpleInfoDTO> cInfo = this.courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> courseMap = CollStreamUtil.toMap(cInfo, CourseSimpleInfoDTO::getId, c -> c);
        //4、收集章、节id用于查询章、节的name
        Set<Long> sectionIds = CollStreamUtil.toSet(records, InteractionQuestion::getSectionId);
        Set<Long> chapterIds = CollStreamUtil.toSet(records, InteractionQuestion::getChapterId);
        HashSet<Long> section_chapter_ids = new HashSet<>(sectionIds.size() + chapterIds.size());
        section_chapter_ids.addAll(sectionIds);
        section_chapter_ids.addAll(chapterIds);
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = this.catalogueClient.batchQueryCatalogue(section_chapter_ids);
        Map<Long, String> catalogueMap = CollStreamUtil.toMap(cataSimpleInfoDTOS, CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName);
        //5、查询提问者的昵称
        Set<Long> userIds = CollStreamUtil.toSet(records, InteractionQuestion::getUserId);
        List<UserDTO> userDTOS = this.userClient.queryUserByIds(userIds);
        Map<Long, String> userMap = CollStreamUtil.toMap(userDTOS, UserDTO::getId, UserDTO::getName);
        //7、转vo
        List<QuestionAdminVO> questionAdminVOList = StreamUtil.of(records).map(r -> {
            QuestionAdminVO questionAdminVO = BeanUtils.copyBean(r, QuestionAdminVO.class);
            //7.1填充章节name
            questionAdminVO.setChapterName(catalogueMap.getOrDefault(r.getChapterId(),""));
            questionAdminVO.setChapterName(catalogueMap.getOrDefault(r.getSectionId(),""));
            //7.2填充课程name、分类名称
            CourseSimpleInfoDTO courseSimpleInfoDTO = courseMap.get(r.getCourseId());
            if (ObjectUtil.isNotEmpty(courseSimpleInfoDTO)){
                questionAdminVO.setCourseName(courseSimpleInfoDTO.getName());
                questionAdminVO.setCategoryName(this.categoryCache.getCategoryNames(courseSimpleInfoDTO.getCategoryIds()));
            }
            //7.4填充用户昵称
            String userName = userMap.get(r.getUserId());
            if (ObjectUtil.isNotEmpty(userName)){
                questionAdminVO.setUserName(userName);
            }
            return questionAdminVO;
        }).collect(Collectors.toList());
        return PageDTO.of(page,questionAdminVOList);
    }

    /**
     * 管理端  根据问题id查询具体问题
     * @param questionAdminId
     * @return
     */
    @Override
    public QuestionAdminVO selectInteractionQuestionAdminById(Long questionAdminId) {
        //1、查询具体问题
        InteractionQuestion interactionQuestion = this.getById(questionAdminId);
        if (ObjectUtil.isEmpty(interactionQuestion)){
            throw new BadRequestException("没有查询到具体的问题~~~~~~");
        }
        //2、更新status为已查看，因为你已经点进去了
        if (ObjectUtil.equal(interactionQuestion.getStatus(),QuestionStatus.UN_CHECK)){
            interactionQuestion.setStatus(QuestionStatus.CHECKED);
            this.updateById(interactionQuestion);
            //2.1、再查出来数据才是真的
            interactionQuestion = this.getById(questionAdminId);
        }
        //3、拷贝属性
        QuestionAdminVO questionAdminVO = BeanUtils.copyBean(interactionQuestion, QuestionAdminVO.class);
        //4、填充提问者的昵称
        UserDTO userDTO = this.userClient.queryUserById(UserContext.getUser());
        if (ObjectUtil.isNotEmpty(userDTO)){
            questionAdminVO.setUserName(userDTO.getName());
        }
        //5、填充课程名称、章名称、节名称、教师
        CourseFullInfoDTO courseInfo = this.courseClient.getCourseInfoById(interactionQuestion.getCourseId(), true, true);
        questionAdminVO.setCourseName(courseInfo.getName());
        List<CataSimpleInfoDTO> chapterDTOS = this.catalogueClient.batchQueryCatalogue(ListUtil.of(interactionQuestion.getChapterId()));
        CataSimpleInfoDTO chapter = null;
        if (ObjectUtil.isNotEmpty(chapterDTOS)){
            chapter = chapterDTOS.get(0);
        }
        List<CataSimpleInfoDTO> sectionDTOS = this.catalogueClient.batchQueryCatalogue(ListUtil.of(interactionQuestion.getSectionId()));
        CataSimpleInfoDTO section = null;
        if (ObjectUtil.isNotEmpty(sectionDTOS)){
            section = sectionDTOS.get(0);
        }
        questionAdminVO.setChapterName(chapter.getName());
        questionAdminVO.setSectionName(section.getName());
        List<UserDTO> userDTOS = this.userClient.queryUserByIds(courseInfo.getTeacherIds());
        if (ObjectUtil.isNotEmpty(userDTOS)){
            StringBuilder sb = new StringBuilder();
            for (UserDTO user : userDTOS){
                sb.append(user.getName() + ", ");
            }
            String teacherName = sb.toString();
            questionAdminVO.setTeacherName(teacherName.substring(0,teacherName.length()-2));
        }
        //6、填充三级分类名称
        questionAdminVO.setCategoryName(this.categoryCache.getCategoryNames(courseInfo.getCategoryIds()));
        return questionAdminVO;
    }

    /**
     * 隐藏或显示问题
     * @param id
     * @param hidden
     */
    @Override
    public void ishidden(Long id, Boolean hidden) {
        //1、直接更新即可
        InteractionQuestion interactionQuestion = InteractionQuestion.builder()
                .id(id)
                .hidden(hidden)
                .build();
        boolean b = this.updateById(interactionQuestion);
        if (!b){
            log.error("隐藏或显示问题功能失败");
        }
    }
    public PageDTO<QuestionVO> interactionQuestionPage(QuestionPageQuery query) {
        // 1.参数校验，课程id和小节id不能都为空
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }
        // 2.分页查询
        Page<InteractionQuestion> page = lambdaQuery()
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();
        // 3.1.得到问题当中的提问者id和最近一次回答的id
        for (InteractionQuestion q : records) {
            if(!q.getAnonymity()) { // 只查询非匿名的问题
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        // 3.2.根据id查询最近一次回答
        answerIds.remove(null);
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if(CollUtils.isNotEmpty(answerIds)) {
            List<InteractionReply> replies = replyMapper.selectBatchIds(answerIds);
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);
                if(!reply.getAnonymity()){
                    userIds.add(reply.getUserId());
                }
            }
        }

        // 3.3.根据id查询用户信息（提问者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 4.封装VO
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            // 4.1.将PO转为VO
            QuestionVO vo = BeanUtils.copyBean(r, QuestionVO.class);
            voList.add(vo);
            // 4.2.封装提问者信息
            if(!r.getAnonymity()){
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }

            // 4.3.封装最近一次回答的信息
            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                if(!reply.getAnonymity()){
                    UserDTO user = userMap.get(reply.getUserId());
                    vo.setLatestReplyUser(user.getName());
                }

            }
        }

        return PageDTO.of(page, voList);
    }
    /**
     * 分页查询问题
     * @param questionPageQuery
     * @return
     */
   /* @Override
    public PageDTO<QuestionVO> interactionQuestionPage(QuestionPageQuery questionPageQuery) {
        //1、参数校验 课程id和小节id必须有一个
        if (ObjectUtil.isEmpty(questionPageQuery.getCourseId()) && ObjectUtil.isEmpty(questionPageQuery.getSectionId())){
            throw new BadRequestException("课程id和小节id必须有一个");
        }
        //2、TODO 分页查询问题  问题不仅仅根据最新时间来排序，用个什么算法来解决？
*//*        Page<InteractionQuestion> iPage = new Page<>(questionPageQuery.getPageNo(), questionPageQuery.getPageSize());
        LambdaQueryWrapper<InteractionQuestion> queryWrapperQuestionPage = Wrappers.<InteractionQuestion>lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))
                .eq(ObjectUtil.equal(questionPageQuery.getOnlyMine(),Boolean.TRUE), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(ObjectUtil.isNotEmpty(questionPageQuery.getCourseId()), InteractionQuestion::getCourseId, questionPageQuery.getCourseId())
                .eq(ObjectUtil.isNotEmpty(questionPageQuery.getSectionId()), InteractionQuestion::getSectionId, questionPageQuery.getSectionId())
                .eq(InteractionQuestion::getHidden,Boolean.FALSE)
                .orderByDesc(InteractionQuestion::getCreateTime);*//*
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))
                .eq(questionPageQuery.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(questionPageQuery.getCourseId() != null, InteractionQuestion::getCourseId, questionPageQuery.getCourseId())
                .eq(questionPageQuery.getSectionId() != null, InteractionQuestion::getSectionId, questionPageQuery.getSectionId())
                .eq(InteractionQuestion::getHidden, false)
                .page(questionPageQuery.toMpPageDefaultSortByCreateTimeDesc());
        //2.1获取分页中的record数据
        List<InteractionQuestion> records = page.getRecords();
        if (ObjectUtil.isEmpty(records)){
            PageDTO.empty(page);
        }
        //3.1、获取提问者的id
        Set<Long> user_ids = StreamUtil.of(records).filter(r -> !r.getAnonymity()).map(InteractionQuestion::getUserId).collect(Collectors.toSet());
        //3.2、获取最新的回答内容的问题的id
        Set<Long> answer_ids = CollStreamUtil.toSet(records, InteractionQuestion::getLatestAnswerId);
        //3.3、根据最新的回答内容的问题的id查询回答内容
        List<InteractionReply> interactionReplies = null;
        if (ObjectUtil.isNotEmpty(answer_ids)){
            interactionReplies = StreamUtil.of(answer_ids).map(this.replyService::getById).collect(Collectors.toList());
        }
        //3.4、构造一个map，key是最新回答内容的id，value是该id对应的内容
        Map<Long, InteractionReply> replyMap = StreamUtil.of(interactionReplies).filter(i -> !i.getAnonymity()).collect(Collectors.toMap(InteractionReply::getId, i -> i));
        //3.5、获取该最新的回答内容的问题的用户id，同时添加到reply_userIds，目的是可以根据用户id查询用户的昵称，头像等等
        Set<Long> reply_userIds = StreamUtil.of(answer_ids).filter(answer_id->!this.replyService.getById(answer_id).getAnonymity()).map(answer_id -> {
            InteractionReply reply = this.replyService.getById(answer_id);
            return reply.getUserId();
        }).collect(Collectors.toSet());
        user_ids.addAll(reply_userIds);
        //4、根据从InteractionQuestion数据库中查出的user_id查出提问者的昵称和头像
        List<UserDTO> userDTOS = null;
        if (ObjectUtil.isNotEmpty(user_ids)){
            userDTOS = this.userClient.queryUserByIds(user_ids);
        }
        //4.1、key是用户的id，value是用户整体信息
        Map<Long, UserDTO> userDTOMap = CollStreamUtil.toMap(userDTOS, UserDTO::getId, u -> u);
        //5、组装VO
        List<QuestionVO> questionVOList = StreamUtil.of(records).map(r -> {
            QuestionVO questionVO = BeanUtils.copyBean(r, QuestionVO.class);
            questionVO.setUserId(null);
            //如果不是匿名的话填充提问者的昵称和头像
            if (ObjectUtil.equal(questionVO.getAnonymity(), Boolean.FALSE)) {
                UserDTO userDTO = userDTOMap.get(r.getUserId());
                if (ObjectUtil.isNotEmpty(userDTO)){
                    questionVO.setUserId(userDTO.getId());
                    questionVO.setUserName(userDTO.getName());
                    questionVO.setUserIcon(userDTO.getIcon());
                }
            }
            //填充最近一次回答的内容
            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if (ObjectUtil.isNotEmpty(reply)){
                questionVO.setLatestReplyContent(reply.getContent());
                if (ObjectUtil.equal(reply.getAnonymity(),Boolean.FALSE)){
                    UserDTO user = userDTOMap.get(reply.getUserId());
                    questionVO.setLatestReplyUser(user.getName());
                }
            }
            return questionVO;
        }).collect(Collectors.toList());
        return PageDTO.of(page,questionVOList);
    }*/
}
