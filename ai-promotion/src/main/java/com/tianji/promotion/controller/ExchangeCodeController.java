package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.service.IExchangeCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 兑换码 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-30
 */
@Api(tags = "兑换码相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/codes")
public class ExchangeCodeController {

    private final IExchangeCodeService exchangeCodeService;

    @ApiOperation("分页查询兑换码")
    @GetMapping("/page")
    public PageDTO<ExchangeCode> queryExchangeCodePage(CodeQuery codeQuery){
        return this.exchangeCodeService.queryExchangeCodePage(codeQuery);
    }
}
