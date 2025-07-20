package com.tianji.promotion.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundGeoOperations;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tianji.promotion.constants.PromotionConstants.*;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-30
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {
    private final StringRedisTemplate redisTemplate;

    private final  BoundValueOperations<String, String> boundedValueOps;

    public ExchangeCodeServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = stringRedisTemplate;
        this.boundedValueOps =  this.redisTemplate.boundValueOps(COUPON_CODE_SERIAL_KEY);
    }

    /**
     * 异步生成兑换码
     * @param coupon
     */
    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        //1、一次性从redis中获取自增的最大数量
        Integer totalNum = coupon.getTotalNum();
        Long increment = this.boundedValueOps.increment(totalNum);
        if (increment == null){
            return;
        }
        int result = increment.intValue();
        //2迭代生成兑换码
        List<ExchangeCode> exchangeCodeList = new ArrayList<>(totalNum);
        for (int i = result - totalNum + 1 ; i < result ; i++){
            //2.1 生成兑换码
            String code = CodeUtil.generateCode(i, coupon.getId());
            //2.2 组装
            ExchangeCode exchangeCode = new ExchangeCode();
            exchangeCode.setId(i);
            exchangeCode.setCode(code);
            exchangeCode.setExchangeTargetId(coupon.getId());
            exchangeCode.setExpiredTime(coupon.getIssueEndTime());
            exchangeCodeList.add(exchangeCode);
        }
        //3、批量插入
        this.saveBatch(exchangeCodeList);
        // 4.写入Redis缓存，member：couponId，score：兑换码的最大序列号
        redisTemplate.opsForZSet().add(COUPON_RANGE_KEY, coupon.getId().toString(), result);
    }

    @Override
    public PageDTO<ExchangeCode> queryExchangeCodePage(CodeQuery codeQuery) {
        //1、查询优惠券下的兑换码
        Page<ExchangeCode> iPage = new Page<>(codeQuery.getPageNo(), codeQuery.getPageSize());
        LambdaQueryWrapper<ExchangeCode> queryWrapper = Wrappers.<ExchangeCode>lambdaQuery()
                .eq(ExchangeCode::getExchangeTargetId, codeQuery.getCouponId())
                .eq(ExchangeCode::getStatus,codeQuery.getStatus());
        Page<ExchangeCode> page = this.page(iPage, queryWrapper);
        if (ObjectUtil.isEmpty(page.getRecords())){
            return PageDTO.empty(iPage);
        }
        return PageDTO.of(iPage,page.getRecords());
    }

    /**
     * 更新校验码在redis中的状态
     * @param id
     * @return
     */
    @Override
    public Boolean updateExchangeMark(long id,Boolean bool) {
        Boolean res = this.redisTemplate.opsForValue().setBit(COUPON_CODE_MAP_KEY, id, Boolean.TRUE);
        return res;
    }

    @Override
    public Long exchangeTargetId(long serialNum) {
        // 1.查询score值比当前序列号大的第一个优惠券
        Set<String> results = redisTemplate.opsForZSet().rangeByScore(
                COUPON_RANGE_KEY, serialNum, serialNum + 5000, 0L, 1L);
        if (CollUtils.isEmpty(results)) {
            return null;
        }
        // 2.数据转换
        String next = results.iterator().next();
        return Long.parseLong(next);
    }
}
