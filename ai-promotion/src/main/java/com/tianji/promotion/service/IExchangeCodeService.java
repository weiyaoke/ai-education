package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CodeQuery;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author ke
 * @since 2025-03-30
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {

    void asyncGenerateCode(Coupon coupon);

    PageDTO<ExchangeCode> queryExchangeCodePage(CodeQuery codeQuery);

    Boolean updateExchangeMark(long id,Boolean bool);
    Long exchangeTargetId(long serialNum);
}
