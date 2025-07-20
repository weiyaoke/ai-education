package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author ke
 * @since 2025-03-30
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("update coupon set issue_num = issue_num + 1 where id = #{id} and issue_num < total_num")
    Integer updateIssueNum(@Param("id") Long id);
}
