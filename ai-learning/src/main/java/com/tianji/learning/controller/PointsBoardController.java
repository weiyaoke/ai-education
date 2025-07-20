package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.service.IPointsRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-26
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/boards")
@Api(tags = "积分相关接口")
public class PointsBoardController {
    private final IPointsBoardService pointsBoardService;
    @ApiOperation("分页查询指定赛季的积分排行榜")
    @GetMapping
    public PointsBoardVO querySeasonPointsBoard(PointsBoardQuery pointsBoardQuery){
        return this.pointsBoardService.querySeasonPointsBoard(pointsBoardQuery);
    }
}
