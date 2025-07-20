package com.tianji.promotion.handler;

import cn.hutool.core.stream.StreamUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendCouponHandler {
    private final ICouponService couponService;
    @XxlJob("sendCouponByTime")
    public void sendCouponByTime(){
        //1、查询出那些处于未开始的，并且发放时间早于当前时间的优惠券
        LocalDateTime dateTime = LocalDateTime.now();
        LambdaQueryWrapper<Coupon> queryWrapper = Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                .le(Coupon::getIssueBeginTime, dateTime);
        List<Coupon> couponList = this.couponService.list(queryWrapper);
        //2、更新状态即可
        List<Coupon> collect = StreamUtil.of(couponList).peek(c -> {
            c.setStatus(CouponStatus.ISSUING);
        }).collect(Collectors.toList());
        //3、批量更新
        this.couponService.updateBatchById(collect);
    }
    @XxlJob("stopSendCouponByTime")
    public void stopSendCouponByTime(){
        //1、查询出那些处于未开始的，并且发放时间早于当前时间的优惠券
        LocalDateTime dateTime = LocalDateTime.now();
        LambdaQueryWrapper<Coupon> queryWrapper = Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .ge(Coupon::getIssueEndTime, dateTime);
        List<Coupon> couponList = this.couponService.list(queryWrapper);
        //2、更新状态即可
        List<Coupon> collect = StreamUtil.of(couponList).peek(c -> {
            c.setStatus(CouponStatus.FINISHED);
        }).collect(Collectors.toList());
        //3、批量更新
        this.couponService.updateBatchById(collect);
    }
}
