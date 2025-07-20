package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author ke
 * @since 2025-03-31
 */
public interface IUserCouponService extends IService<UserCoupon> {

    void saveUserCoupon(Long id);

    void checkAndCreateUserCoupon(UserCouponDTO uc);

    void exchangeCodeUserCoupon(String code);

    PageDTO<CouponVO> queryUserCoupon(UserCouponQuery couponQuery);
}
