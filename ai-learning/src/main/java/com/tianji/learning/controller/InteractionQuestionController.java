package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-21
 */
@Api(tags = "互动问题接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class InteractionQuestionController {

    private final IInteractionQuestionService interactionQuestionService;

    @ApiOperation("新增提问")
    @PostMapping
    public void saveInteractionQuestion(@Valid @RequestBody QuestionFormDTO questionFormDTO){
        this.interactionQuestionService.saveInteractionQuestion(questionFormDTO);
    }

    @ApiOperation("分页查询互动问题")
    @GetMapping("/page")
    public PageDTO<QuestionVO> interactionQuestionPage(QuestionPageQuery questionPageQuery){
        return this.interactionQuestionService.interactionQuestionPage(questionPageQuery);
    }

    @ApiOperation("根据id查询互动问题")
    @GetMapping("{id}")
    public QuestionVO selectInteractionQuestionById(@PathVariable String id){
        return this.interactionQuestionService.selectInteractionQuestionById(id);
    }
}
