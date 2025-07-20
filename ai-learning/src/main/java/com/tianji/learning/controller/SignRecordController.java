package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Api(tags = "签到相关接口")
@RestController
@RequestMapping("sign-records")
public class SignRecordController {
    private final ISignRecordService signRecordService;
    @ApiOperation("添加签到记录")
    @PostMapping
    public SignResultVO addSign(){
        return this.signRecordService.addSign();
    }
    @ApiOperation("查询签到记录")
    @GetMapping
    public List<Integer> selectSign(){
        return this.signRecordService.selectSign();
    }

}
