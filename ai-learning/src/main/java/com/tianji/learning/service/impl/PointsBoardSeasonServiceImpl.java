package com.tianji.learning.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ke
 * @since 2025-03-26
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {
    /**
     * 根据日期来查询赛季
     * @param dateTime
     * @return
     */
    @Override
    public Integer queryPointsBoardSeasonByTime(LocalDateTime dateTime) {
        Optional<PointsBoardSeason> optional = lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, dateTime)
                .ge(PointsBoardSeason::getEndTime, dateTime).oneOpt();
        return optional.map(PointsBoardSeason::getId).orElse(null);
    }
    /**
     * 查询赛季列表
     * @return
     */
    @Override
    public List<PointsBoardSeason> queryPointsBoardSeason() {
        List<PointsBoardSeason> list = this.list();
        if (ObjectUtil.isEmpty(list)){
            return ListUtil.empty();
        }
        return list;
    }
}
