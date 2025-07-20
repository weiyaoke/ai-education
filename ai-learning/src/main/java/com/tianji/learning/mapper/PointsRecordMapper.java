package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domain.po.TypePointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 *
 * @author ke
 * @since 2025-03-26
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    @Select("SELECT SUM(points) FROM points_record ${ew.customSqlSegment}")
    Integer queryUserPointsByTypeAndDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);

/*    @Select("SELECT type , SUM(points) AS points FROM points_record ${ew.customSqlSegment} GROUP BY type")
    List<PointsRecord> queryUserTodayPoints(@Param(Constants.WRAPPER) LambdaQueryWrapper<PointsRecord> wrapper);*/
    @Select("SELECT type , SUM(points) AS points FROM points_record ${ew.customSqlSegment} GROUP BY type")
    List<TypePointsRecord> queryUserPointsByDate(@Param(Constants.WRAPPER) LambdaQueryWrapper<PointsRecord> wrapper);

    void createPointsBoardTableBySeason(@Param("tableName") String tableName);
}
