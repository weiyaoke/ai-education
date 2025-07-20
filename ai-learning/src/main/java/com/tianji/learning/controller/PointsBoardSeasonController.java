package com.tianji.learning.controller;


import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.service.IPointsBoardSeasonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-26
 */
@RestController
@RequestMapping("/boards/season")
@Api(tags = "查询赛季列表功能")
@RequiredArgsConstructor
public class PointsBoardSeasonController {
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    @ApiOperation("查询赛季列表")
    @GetMapping("/list")
    public List<PointsBoardSeason> queryPointsBoardSeason(){
        return this.pointsBoardSeasonService.queryPointsBoardSeason();
    }

}
