package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
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
@Api(tags = "管理端互动问题回答回复接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/replies")
public class InteractionReplyAdminController {
    private final IInteractionReplyService replyService;

    @ApiOperation("管理端分页查询问题下的回答列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> page(ReplyPageQuery replyPageQuery){
        return this.replyService.selectPage(replyPageQuery,true);
    }
    @ApiOperation("管理端隐藏或显示回答回复")
    @PutMapping("/{id}/hidden/{hidden}")
    public void ishidden(@PathVariable("id") Long id,@PathVariable("hidden") Boolean hidden){
        this.replyService.ishidden(id,hidden);
    }
}
