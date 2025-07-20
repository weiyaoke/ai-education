package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-21
 */
@Api(tags = "管理端互动问题接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/questions")
public class InteractionQuestionAdminController {

    private final IInteractionQuestionService interactionQuestionService;

    @ApiOperation("分页查询互动问题")
    @GetMapping("/page")
    public PageDTO<QuestionAdminVO> interactionQuestionAdminPage(QuestionAdminPageQuery questionAdminPageQuery){
        return this.interactionQuestionService.interactionQuestionAdminPage(questionAdminPageQuery);
    }
    @ApiOperation("根据id查询互动问题")
    @GetMapping("/{id}")
    public QuestionAdminVO selectInteractionQuestionAdminById(@PathVariable("id") Long questionAdminId){
        return this.interactionQuestionService.selectInteractionQuestionAdminById(questionAdminId);
    }
    @ApiOperation("管理端隐藏或显示问题")
    @PutMapping("/{id}/hidden/{hidden}")
    public void ishidden(@PathVariable("id") Long id,@PathVariable("hidden") Boolean hidden){
        this.interactionQuestionService.ishidden(id,hidden);
    }
}
