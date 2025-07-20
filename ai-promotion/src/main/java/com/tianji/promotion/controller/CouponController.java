package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author ke
 * @since 2025-03-30
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Api(tags = "优惠券相关接口")
public class CouponController {

    private final ICouponService couponService;

    @ApiOperation("新增优惠券")
    @PostMapping
    public void saveCoupon(@Valid @RequestBody CouponFormDTO dto){
        this.couponService.saveCoupon(dto);
    }

    @ApiOperation("分页查询优惠券")
    @GetMapping("/page")
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query){
        return this.couponService.queryCouponByPage(query);
    }
    @ApiOperation("发放优惠券")
    @PutMapping("/{id}/issue")
    public void sendCoupon(@RequestBody @Valid CouponIssueFormDTO dto){
        this.couponService.sendCoupon(dto);
    }

    @ApiOperation("根据id查询优惠券")
    @GetMapping("/{id}")
    public CouponDetailVO queryCouponById(@PathVariable Long id){
        return this.couponService.queryCouponById(id);
    }

    @ApiOperation("修改优惠券")
    @PutMapping("/{id}")
    public void updateCoupon(Coupon coupon){
        this.couponService.updateCoupon(coupon);
    }

    @ApiOperation("删除优惠券")
    @DeleteMapping("/{id}")
    public void deleteCoupon(@PathVariable Long id){
        this.couponService.deleteCoupon(id);
    }

    @ApiOperation("暂停发放优惠券")
    @PutMapping("/{id}/pause")
    public void pauseSendCoupon(@PathVariable Long id){
        this.couponService.pauseSendCoupon(id);
    }

    @ApiOperation("查询发放中的优惠券")
    @GetMapping("/list")
    public List<CouponVO> queryIssueCoupon(){
        return this.couponService.queryIssuingCoupon();
    }

}
