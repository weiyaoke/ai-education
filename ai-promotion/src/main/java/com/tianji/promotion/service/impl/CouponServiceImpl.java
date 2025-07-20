package com.tianji.promotion.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tianji.promotion.constants.PromotionConstants.COUPON_CACHE_KEY_PREFIX;
import static com.tianji.promotion.enums.CouponStatus.*;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-30
 */
@RequiredArgsConstructor
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {
    private final ICouponScopeService couponScopeService;
    private final IExchangeCodeService codeService;
    private final CategoryClient categoryClient;
    private final IUserCouponService userCouponService;
    private final StringRedisTemplate redisTemplate;
    /**
     * 新增优惠券
     * @param dto
     */
    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        //1、保存优惠券到数据库
        //1、1、vo转po
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        this.save(coupon);
        //2、保存优惠券的使用范围到数据库
        if (!dto.getSpecific()){
            return;
        }
        //2.2 获取优惠券的使用范围
        List<Long> scopes = dto.getScopes();
        if (ObjectUtil.isEmpty(scopes)){
            throw new BadRequestException("优惠券的使用范围不能为空！！！");
        }
        //2.3 获取优惠券id
        Long couponId = coupon.getId();
        List<CouponScope> couponScopes = StreamUtil.of(scopes)
                .map(scope -> new CouponScope().setCouponId(couponId).setBizId(scope))
                .collect(Collectors.toList());
        this.couponScopeService.saveBatch(couponScopes);
    }

    /**
     * 分页查询优惠券
     * @param query
     * @return
     */
    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        //1、构造分页条件
        Page<Coupon> iPage = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<Coupon> queryWrapper = Wrappers.<Coupon>lambdaQuery()
                .like(ObjectUtil.isNotEmpty(query.getName()), Coupon::getName, query.getName())
                .eq(ObjectUtil.isNotEmpty(query.getType()), Coupon::getDiscountType, query.getType())
                .eq(ObjectUtil.isNotEmpty(query.getStatus()), Coupon::getStatus, query.getStatus())
                .orderByDesc(Coupon::getCreateTime);
        //2、查询数据库
        Page<Coupon> page = this.page(iPage, queryWrapper);
        List<Coupon> couponList = page.getRecords();
        if (ObjectUtil.isEmpty(couponList)){
            return PageDTO.empty(iPage);
        }
        //3、构造vo
        List<CouponPageVO> voList = BeanUtils.copyList(couponList, CouponPageVO.class);
        //4、返回
        return PageDTO.of(iPage,voList);
    }

    /**
     * 根据id查询优惠券
     * @param id
     */
    @Override
    public CouponDetailVO queryCouponById(Long id) {
        //1、查询优惠券的信息
        Coupon coupon = this.getById(id);
        //2、拷贝属性
        if (ObjectUtil.isEmpty(coupon)){
            return null;
        }
        CouponDetailVO couponDetailVO = BeanUtils.copyBean(coupon, CouponDetailVO.class);
        //3、如果不限定分类
        if (!coupon.getSpecific()){
            return couponDetailVO;
        }
        //4、查询具体的分类信息
        List<CategoryBasicDTO> allOfOneLevel = this.categoryClient.getAllOfOneLevel();
        Map<Long, String> map = CollStreamUtil.toMap(allOfOneLevel, CategoryBasicDTO::getId, CategoryBasicDTO::getName);
        //5、查询优惠券的使用范围
        LambdaQueryWrapper<CouponScope> queryWrapper = Wrappers.<CouponScope>lambdaQuery().eq(CouponScope::getCouponId, coupon.getId());
        List<CouponScope> couponScopeList = this.couponScopeService.list(queryWrapper);
        if (ObjectUtil.isEmpty(couponScopeList)){
            return null;
        }
        List<CouponScopeVO> couponScopeVOS = StreamUtil.of(couponScopeList).map(c -> {
            CouponScopeVO couponScopeVO = new CouponScopeVO();
            couponScopeVO.setId(c.getBizId());
            couponScopeVO.setName(map.get(c.getBizId()));
            return couponScopeVO;
        }).collect(Collectors.toList());
        couponDetailVO.setScopes(couponScopeVOS);
        return couponDetailVO;
    }

    /**
     * 修改优惠券
     * @param coupon
     */
    @Override
    public void updateCoupon(Coupon coupon) {
        Coupon c = this.getById(coupon.getId());
        if (ObjectUtil.notEqual(c.getStatus(), UN_ISSUE)){
            throw new BadRequestException("优惠券不属于待发放状态，不能修改优惠券");
        }
        this.updateById(coupon);
    }

    /**
     * 删除优惠券
     * @param id
     */
    @Override
    public void deleteCoupon(Long id) {
        //如果优惠券是待发放状态就可以删除
        Coupon coupon = this.getById(id);
        if (ObjectUtil.notEqual(coupon.getStatus(),CouponStatus.DRAFT)){
            throw new BadRequestException("优惠券不是待发放状态，不可以删除");
        }
        this.removeById(id);
    }

    /**
     * 暂停发放优惠券
     * @param id
     */
    @Transactional
    @Override
    public void pauseSendCoupon(Long id) {
        // 1.查询旧优惠券
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }

        // 2.当前券状态必须是未开始或进行中
        CouponStatus status = coupon.getStatus();
        if (status != UN_ISSUE && status != ISSUING) {
            // 状态错误，直接结束
            return;
        }

        // 3.更新状态
        boolean success = lambdaUpdate()
                .set(Coupon::getStatus, PAUSE)
                .eq(Coupon::getId, id)
                .in(Coupon::getStatus, UN_ISSUE, ISSUING)
                .update();
        if (!success) {
            // 可能是重复更新，结束
            log.error("重复暂停优惠券");
        }

        // 4.删除缓存
        redisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id);
    }

    /**
     * 查询发放中的优惠券
     * @return
     */
    @Override
    public List<CouponVO> queryIssuingCoupon() {
        //1、查询发放中的优惠券  连个查询条件 1、发放中 2、手动领取
        LambdaQueryWrapper<Coupon> queryWrapper = Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC);
        List<Coupon> couponList = this.list(queryWrapper);
        if (ObjectUtil.isEmpty(couponList)){
            return ListUtil.empty();
        }
        List<Long> couponIds = CollStreamUtil.toList(couponList, Coupon::getId);
        //2、查看是否可领取  1、已经领取优惠券的数量 < 优惠券总数 && 2、用户领取的数量 < 优惠券规定的每人领取上限
        //查询用户领取每张优惠券对应的数量
        LambdaQueryWrapper<UserCoupon> userCouponWrapper = Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId,couponIds);
        List<UserCoupon> userCoupons = this.userCouponService.list(userCouponWrapper);
        Map<Long, Long> issuedMap = CollStreamUtil.groupBy(userCoupons, UserCoupon::getCouponId, Collectors.counting());
        //3、查看是否已领取 1、用户已经领取优惠券 && 2、用户未使用该优惠券
        //查询用户领取的未使用的优惠券对应的数量
        Map<Long, Long> unUsedMap = StreamUtil.of(userCoupons)
                .filter(uc -> ObjectUtil.equal(uc.getStatus(), UserCouponStatus.UNUSED))
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        //4、组装vo
        List<CouponVO> voList = StreamUtil.of(couponList).map(coupon -> {
            CouponVO couponVO = BeanUtils.copyBean(coupon, CouponVO.class);
            couponVO.setAvailable(coupon.getIssueNum() < coupon.getTotalNum() && issuedMap.getOrDefault(coupon.getId(), 0l) < coupon.getUserLimit());
            couponVO.setReceived(unUsedMap.getOrDefault(coupon.getId(), 0l) > 0);
            return couponVO;
        }).collect(Collectors.toList());
        return voList;
    }

    /**
     * 发放优惠券
     * @param dto
     */
    @Transactional
    @Override
    public void sendCoupon(CouponIssueFormDTO dto) {
        //1、先查询优惠券
        Coupon coupon = this.getById(dto.getId());
        if (ObjectUtil.isEmpty(coupon)){
            throw new BadRequestException("优惠券不存在");
        }
        //2、查询优惠券的状态判断是否符合发放状态
        if (ObjectUtil.notEqual(coupon.getStatus(), CouponStatus.DRAFT) && ObjectUtil.notEqual(coupon.getStatus(), PAUSE)){
            throw new BadRequestException("优惠券不符合发放状态");
        }
        //3、判断优惠券是否是立即发放还是定时发放
        LocalDateTime dateTime = LocalDateTime.now();
        Boolean isBegin = ObjectUtil.isEmpty(dto.getIssueBeginTime()) || !dateTime.isBefore(dto.getIssueBeginTime());
        //3.0 拷贝属性
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        //3.1 立即发放
        if (isBegin){
            //更改状态
            c.setStatus(ISSUING);
            c.setIssueBeginTime(dateTime);
        }else{
            //定时发放
            c.setStatus(UN_ISSUE);
        }
        //5、更新
        this.updateById(c);
        // 6.添加缓存，前提是立刻发放的
        if (isBegin) {
            coupon.setIssueBeginTime(c.getIssueBeginTime());
            coupon.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupon);
        }
        // 7.判断是否需要生成兑换码，优惠券类型必须是兑换码，优惠券状态必须是待发放
        if(coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT){
            coupon.setIssueEndTime(c.getIssueEndTime());
            this.codeService.asyncGenerateCode(coupon);
        }
    }
    private void cacheCouponInfo(Coupon coupon) {
        // 1.组织数据
        Map<String, String> map = new HashMap<>(4);
        map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueBeginTime())));
        map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueEndTime())));
        map.put("totalNum", String.valueOf(coupon.getTotalNum()));
        map.put("userLimit", String.valueOf(coupon.getUserLimit()));
        // 2.写缓存
        redisTemplate.opsForHash().putAll(PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupon.getId(), map);
    }
}
