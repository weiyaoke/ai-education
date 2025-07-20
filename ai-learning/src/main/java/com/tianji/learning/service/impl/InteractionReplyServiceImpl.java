package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-21
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper questionMapper;
    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;
    private final RemarkClient remarkClient;
    private final RabbitMqHelper mqHelper;

    /**
     * 新增回答回复接口
     * @param replyDTO
     */
    @Override
    @Transactional
    public void saveReply(ReplyDTO replyDTO) {
        //1、写入回答评论表
        InteractionReply interactionReply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        interactionReply.setUserId(UserContext.getUser());
        boolean save = this.save(interactionReply);
        if (!save){
            log.error("新增回答评论表异常");
            return;
        }
        //2判断是否是回答
        if (ObjectUtil.isEmpty(replyDTO.getAnswerId())){
            //2.1、如果是回答的话修改最近一次回答的id
            InteractionQuestion interactionQuestion = InteractionQuestion.builder()
                    .id(replyDTO.getQuestionId())
                    .latestAnswerId(interactionReply.getId())
                    .build();
            Integer updateQuestionLatestAnswerId = this.questionMapper.updateById(interactionQuestion);
            if (ObjectUtil.equal(updateQuestionLatestAnswerId,0)){
                log.error("更新问题表最近一次回答的id失败");
                return;
            }
            //2.2 累加问题表的回答次数
            InteractionQuestion question = this.questionMapper.getById(replyDTO.getQuestionId());
            if (ObjectUtil.isNotEmpty(question)){
                question.setAnswerTimes(question.getAnswerTimes() + 1);
                Integer updateQuestionAnswerTimes = this.questionMapper.updateById(question);
                if (ObjectUtil.equal(updateQuestionAnswerTimes,0)){
                    log.error("更新问题表累加问题表的回答次数失败");
                    return;
                }
            }
        }else {
            //2.3、如果是回复的话累加回答下的评论次数
            InteractionReply reply = this.getById(replyDTO.getAnswerId());
            if (ObjectUtil.isEmpty(reply)){
                log.error("累加回答下的评论次数失败");
                return;
            }
            reply.setReplyTimes(reply.getReplyTimes() + 1);
            boolean updateReplyTimes = this.updateById(reply);
            if (!updateReplyTimes){
                log.error("更新累加回答下的评论次数失败");
                return;
            }
            //2.5 累加问题表的回答次数
            InteractionQuestion question = this.questionMapper.getById(replyDTO.getQuestionId());
            if (ObjectUtil.isNotEmpty(question)){
                question.setAnswerTimes(question.getAnswerTimes() + 1);
                Integer updateQuestionAnswerTimes = this.questionMapper.updateById(question);
                if (ObjectUtil.equal(updateQuestionAnswerTimes,0)){
                    log.error("更新问题表累加问题表的回答次数失败");
                    return;
                }
            }
        }
        //3、判断是否是学生提交
        if (replyDTO.getIsStudent()){
            //3.1、如果是的话则修改问题表状态是未查看
            InteractionQuestion interactionQuestion = InteractionQuestion.builder()
                    .id(replyDTO.getQuestionId())
                    .status(QuestionStatus.UN_CHECK)
                    .build();
            Integer updateInteractionQuestionStatus = this.questionMapper.updateById(interactionQuestion);
            if (ObjectUtil.equal(updateInteractionQuestionStatus,0)){
                log.error("修改问题表状态是未查看失败");
                return;
            }
            //3.2发送消息加积分
            this.mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,MqConstants.Key.WRITE_REPLY,UserContext.getUser());
        }
    }

    /**
     * 分页查询问题下的回答列表
     * @param replyPageQuery
     */
    @Override
    public PageDTO<ReplyVO> selectPage(ReplyPageQuery replyPageQuery,Boolean isAdmin) {
        if (ObjectUtil.isEmpty(replyPageQuery.getQuestionId()) && ObjectUtil.isEmpty(replyPageQuery.getAnswerId())){
            log.error("问题id和回答id不能都为空！！！");
            throw new BadRequestException("问题id和回答id不能都为空！！！");
        }
        //1、标志当前是否是查询回答状态
        Boolean isQueryAnswer = ObjectUtil.isNotEmpty(replyPageQuery.getQuestionId());
        //2、执行分页查询
        Page<InteractionReply> iPage = new Page<>(replyPageQuery.getPageNo(), replyPageQuery.getPageSize());
        LambdaQueryWrapper<InteractionReply> queryWrapper = Wrappers.<InteractionReply>lambdaQuery()
                .eq(isQueryAnswer, InteractionReply::getQuestionId, replyPageQuery.getQuestionId())
                .eq(InteractionReply::getAnswerId, isQueryAnswer ? 0 : replyPageQuery.getAnswerId())
                .eq(!isAdmin,InteractionReply::getHidden,Boolean.FALSE)
                .orderByDesc(InteractionReply::getLikedTimes)
                .orderByDesc(InteractionReply::getCreateTime);
        Page<InteractionReply> replyPage = this.page(iPage, queryWrapper);
        List<InteractionReply> records = replyPage.getRecords();
        if (ObjectUtil.isEmpty(records)){
            return PageDTO.empty(replyPage);
        }
        //3、收集提问者的id，目标回复的id
        Set<Long> userIds = StreamUtil.of(records).filter(Predicate.not(InteractionReply::getAnonymity).or(r->isAdmin))
                .map(InteractionReply::getUserId).collect(Collectors.toSet());
        Set<Long> targetReplyIds = CollStreamUtil.toSet(records, InteractionReply::getTargetReplyId);
        targetReplyIds.remove(0l);
        //3.1 根据目标回复的id查询目标回复的用户id
        if (ObjectUtil.isNotEmpty(targetReplyIds)){
            List<InteractionReply> replies = this.listByIds(targetReplyIds);
            Set<Long> targetReplyUserIds = StreamUtil.of(replies).filter(Predicate.not(InteractionReply::getAnonymity).or(r->isAdmin))
                    .map(InteractionReply::getUserId).collect(Collectors.toSet());
            userIds.addAll(targetReplyUserIds);
        }
        //3.2 查询用户的详细信息
        Map<Long, UserDTO> userDTOMap = null;
        if (ObjectUtil.isNotEmpty(userIds)){
            List<UserDTO> userDTOS = this.userClient.queryUserByIds(userIds);
            userDTOMap = CollStreamUtil.toMap(userDTOS, UserDTO::getId, u -> u);
        }
        //3.3 查询自己是否点过赞的业务ids
        Set<Long> bizIds = CollStreamUtil.toSet(records, InteractionReply::getId);
        Set<Long> isBizIds = this.remarkClient.selectLikeStatus(bizIds);
        //4、组转vo
        ArrayList<ReplyVO> voList = new ArrayList<>(records.size());
        for (InteractionReply reply : records){
            ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);
            voList.add(vo);
            //4.1填充提问者的信息
            if (!reply.getAnonymity() || isAdmin){
                UserDTO userDTO = userDTOMap.get(reply.getUserId());
                if (ObjectUtil.isNotEmpty(userDTO)){
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                    vo.setUserType(userDTO.getType());
                }
            }
            //4.2 如果是回复的话则需要填充目标回复者的名字
            if (ObjectUtil.isNotEmpty(targetReplyIds)){
                UserDTO userDTO = userDTOMap.get(reply.getTargetReplyId());
                if (ObjectUtil.isNotEmpty(userDTO)){
                    vo.setTargetUserName(userDTO.getName());
                }
            }
            //4.3查询是否自己点过赞
            vo.setLiked(isBizIds.contains(reply.getId()));
        }
        return PageDTO.of(replyPage,voList);
    }

    /**
     *理端显示或隐藏评论
     * @param id
     * @param hidden
     */
    @Override
    public void ishidden(Long id, Boolean hidden) {
        //1、直接更新即可
        InteractionReply interactionReply = InteractionReply.builder()
                .id(id)
                .hidden(hidden)
                .build();
        boolean b = this.updateById(interactionReply);
        if (!b){
            log.error("理端显示或隐藏评论功能故障");
            return;
        }
    }
}
