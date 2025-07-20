package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-21
 */
@Api(tags = "互动问题回答回复接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/replies")
public class InteractionReplyController {
    private final IInteractionReplyService replyService;
    @ApiOperation("新增回复回答")
    @PostMapping
    public void saveReply(@RequestBody ReplyDTO replyDTO){
        this.replyService.saveReply(replyDTO);
    }
    @ApiOperation("分页查询问题下的回答列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> page(ReplyPageQuery replyPageQuery){
        return this.replyService.selectPage(replyPageQuery,false);
    }
}
