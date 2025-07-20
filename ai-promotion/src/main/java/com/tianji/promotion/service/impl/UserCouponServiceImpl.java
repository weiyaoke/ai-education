package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.autoconfigure.redisson.annotations.Lock;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.UserCouponQuery;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import com.tianji.promotion.utils.MyLock;
import com.tianji.promotion.utils.RedisLock;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-31
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
    private final CouponMapper couponMapper;
    private final IExchangeCodeService exchangeCodeService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;

    /**
     * 新增用户券
     * @param id
     */
    @Lock(name = "lock:coupon:#{id}")
    @Override
    public void saveUserCoupon(Long id) {
        Long userId = UserContext.getUser();
        //1、查询优惠券判断是否存在
        // 1.查询优惠券
        Coupon coupon = queryCouponByCache(id);
        if (ObjectUtil.isEmpty(coupon)){
            throw new BadRequestException("优惠券不存在！！！");
        }
        //2、校验优惠券的时间 是不是正在发放中
        LocalDateTime dateTime = LocalDateTime.now();
        if (dateTime.isBefore(coupon.getIssueBeginTime()) || dateTime.isAfter(coupon.getIssueEndTime())){
            throw new BadRequestException("不在规定的优惠券领取时间范围之内");
        }
        //3、校验优惠券剩余库存是否充足
        if (coupon.getTotalNum() <= 0){
            throw new BadRequestException("优惠券已领完");
        }
        // 4.校验每人限领数量
        // 4.1.查询领取数量
        String key = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + id;
        Long count = redisTemplate.opsForHash().increment(key, userId.toString(), 1);
        // 4.2.校验限领数量
        if(count > coupon.getUserLimit()){
            throw new BadRequestException("超出领取数量");
        }
        // 5.扣减优惠券库存
        redisTemplate.opsForHash().increment(
                PromotionConstants.COUPON_CACHE_KEY_PREFIX + id, "totalNum", -1);

        // 6.发送MQ消息
        UserCouponDTO uc = new UserCouponDTO();
        uc.setUserId(userId);
        uc.setCouponId(id);
        mqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, uc);
    }

    private Coupon queryCouponByCache(Long couponId) {
        // 1.准备KEY
        String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId;
        // 2.查询
        Map<Object, Object> objMap = redisTemplate.opsForHash().entries(key);
        if (objMap.isEmpty()) {
            return null;
        }
        // 3.数据反序列化
        return BeanUtils.mapToBean(objMap, Coupon.class, false, CopyOptions.create());
    }

    @Transactional
    @Override
    public void checkAndCreateUserCoupon(UserCouponDTO uc){
        //1、校验优惠券的每人限领数量
        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在！");
        }
        //2、优惠券已经发的数量+1
        Integer res = this.couponMapper.updateIssueNum(coupon.getId());
        if (res == 0){
            throw new BizIllegalException("优惠券库存不足！");
        }
        // 3.新增一个用户券
        this.saveUserCouponInfo(coupon, uc.getUserId());

        //5、更新兑换码状态
        if (ObjectUtil.isNotEmpty(uc.getSerialNum())){
            this.exchangeCodeService.lambdaUpdate()
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .set(ExchangeCode::getUserId,uc.getUserId())
                    .eq(ExchangeCode::getId,uc.getSerialNum())
                    .update();
        }
    }
    private void saveUserCouponInfo(Coupon coupon, Long userId) {
        LocalDateTime dateTime = LocalDateTime.now();
        //3、生成用户券
        UserCoupon userCoupon = new UserCoupon();
        //4.1填充基本信息
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        //4.2填充有效时间
        if (ObjectUtil.isEmpty(coupon.getTermBeginTime())){
            //有效期是按天数存储的
            userCoupon.setTermBeginTime(dateTime);
            userCoupon.setTermEndTime(dateTime.plusDays(coupon.getTermDays()));
        }else{
            userCoupon.setTermBeginTime(coupon.getTermBeginTime());
            userCoupon.setTermEndTime(coupon.getTermEndTime());
        }
        //6、保存
        this.save(userCoupon);
    }
    /**
     * 兑换优惠券
     * @param code
     */
    @Lock(name = "lock:coupon:code:#{code}")
    @Override
    public void exchangeCodeUserCoupon(String code) {
        // 1.校验并解析兑换码
        long serialNum = CodeUtil.parseCode(code);
        // 2.校验是否已经兑换 SETBIT KEY 4 1
        boolean exchanged = exchangeCodeService.updateExchangeMark(serialNum, true);
        if (exchanged) {
            throw new BizIllegalException("兑换码已经被兑换过了");
        }
        try {
            // 3.查询兑换码对应的优惠券id
            Long couponId = exchangeCodeService.exchangeTargetId(serialNum);
            if (couponId == null) {
                throw new BizIllegalException("兑换码不存在！");
            }
            Coupon coupon = queryCouponByCache(couponId);
            // 4.是否过期
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(coupon.getIssueEndTime()) || now.isBefore(coupon.getIssueBeginTime())) {
                throw new BizIllegalException("优惠券活动未开始或已经结束");
            }

            // 5.校验每人限领数量
            Long userId = UserContext.getUser();
            // 5.1.查询领取数量
            String key = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponId;
            Long count = redisTemplate.opsForHash().increment(key, userId.toString(), 1);
            // 5.2.校验限领数量
            if(count > coupon.getUserLimit()){
                throw new BadRequestException("超出领取数量");
            }

            // 6.发送MQ消息通知
            UserCouponDTO uc = new UserCouponDTO();
            uc.setUserId(userId);
            uc.setCouponId(couponId);
            uc.setSerialNum((int)serialNum);
            mqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE, MqConstants.Key.COUPON_RECEIVE, uc);
        } catch (Exception e) {
            // 重置兑换的标记 0
            exchangeCodeService.updateExchangeMark(serialNum, false);
            throw e;
        }
    }

    /**
     * 分页查询我的优惠券
     * @param couponQuery
     */
    @Override
    public PageDTO<CouponVO> queryUserCoupon(UserCouponQuery couponQuery) {
        //根据优惠券的使用状态查询我的优惠券
        Page<UserCoupon> iPage = new Page<>(couponQuery.getPageNo(), couponQuery.getPageSize());
        LambdaQueryWrapper<UserCoupon> wrapper = Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .eq(UserCoupon::getStatus, couponQuery.getStatus());
        Page<UserCoupon> page = this.page(iPage, wrapper);
        List<UserCoupon> records = page.getRecords();
        if (ObjectUtil.isEmpty(records)){
            return PageDTO.empty(iPage);
        }
        //查询优惠券的ids
        List<Long> couponIds = StreamUtil.of(records).map(UserCoupon::getCouponId).collect(Collectors.toList());
        List<Coupon> coupons = this.couponMapper.selectBatchIds(couponIds);
        //拷贝属性
        List<CouponVO> couponVOS = BeanUtils.copyList(coupons, CouponVO.class);
        return PageDTO.of(iPage,couponVOS);
    }
}
