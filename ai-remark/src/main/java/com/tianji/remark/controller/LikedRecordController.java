package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-25
 */
@Api(tags = "评价点赞功能模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
public class LikedRecordController {
    private final ILikedRecordService likedRecordService;
    @ApiOperation("新增或取消点赞")
    @PostMapping
    public void addLikeTimes(@Valid @RequestBody LikeRecordFormDTO likeRecordDTO){
        this.likedRecordService.addLikeTimes(likeRecordDTO);
    }
    @ApiOperation("批量查询点赞状态")
    @GetMapping("/list")
    public Set<Long> selectLikeStatus(@RequestParam("bizIds") List<Long> bizIds){
        return this.likedRecordService.selectLikeStatus(bizIds);
    }
}
