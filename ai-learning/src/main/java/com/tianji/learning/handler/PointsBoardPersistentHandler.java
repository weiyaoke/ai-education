package com.tianji.learning.handler;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.learning.constants.RedisConstants.*;

@Component
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    private final IPointsBoardService pointsBoardService;
    private final StringRedisTemplate redisTemplate;
    private final IPointsRecordService pointsRecordService;
    @XxlJob("createTableJob")
    public void createPointsBoardTableOfLastSeason(){
        //1、获取上赛季得分榜，判断是第几赛季
        LocalDateTime dateTime = LocalDateTime.now().minusDays(5);
        Integer season = this.pointsBoardSeasonService.queryPointsBoardSeasonByTime(dateTime);
        if (ObjectUtil.isEmpty(season)){
            return;
        }
        //2、创建表
        this.pointsBoardService.createPointsBoardBySeason(season);
    }
    @XxlJob("savePointsBoard2Db")
    public void savePointsBoard2Db(){
        //1、获取上个月时间
        LocalDateTime dateTime = LocalDateTime.now().minusDays(5);
        //2、查询redis中上赛季的数据
        String key = POINTS_BOARD_KEY_PREFIX + dateTime.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        //3、计算动态表名
        Integer season = this.pointsBoardSeasonService.queryPointsBoardSeasonByTime(dateTime);
        if (ObjectUtil.isEmpty(season)){
            return;
        }
        //4、保存到线程池中
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + season);
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        Integer pageNo = shardIndex + 1;
        Integer pageSize = 1000;
        while(true){
            //5、分页查询得分榜信息
            List<PointsBoard> userPointsBoard = this.pointsBoardService.queryUserSeasonInfoFromRedis(key, pageNo, pageSize);
            if (ObjectUtil.isEmpty(userPointsBoard)){
                break;
            }
            //6、保存到数据库
            List<PointsBoard> result = StreamUtil.of(userPointsBoard).peek(u -> {
                u.setId(u.getRank().longValue());
                u.setRank(null);
            }).collect(Collectors.toList());
            this.pointsBoardService.saveBatch(result);
            pageNo+=shardTotal;
        }
        //7、任务结束，移除动态表名
        TableInfoContext.remove();
    }
    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis(){
        LocalDateTime dateTime = LocalDateTime.now().minusDays(5);
        String key = POINTS_BOARD_KEY_PREFIX + dateTime.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        this.redisTemplate.unlink(key);
    }
    @XxlJob("createPointsBoardTableBySeason")
    public void createPointsBoardTableBySeason(){
        //1、获取上赛季信息
        LocalDateTime dateTime = LocalDateTime.now().minusDays(5);
        Integer season = this.pointsBoardSeasonService.queryPointsBoardSeasonByTime(dateTime);
        if (ObjectUtil.isEmpty(season)){
            return;
        }
        this.pointsRecordService.createPointsBoardTableBySeason(season);
    }

    @XxlJob("savePointsBoardToOtherTable")
    public void savePointsBoardToOtherTable(){
        //1、获取上赛季信息
        LocalDateTime dateTime = LocalDateTime.now().minusDays(5);
        Integer season = this.pointsBoardSeasonService.queryPointsBoardSeasonByTime(dateTime);
        //2、分页查询
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        int pageNo = shardIndex + 1;
        int pageSize = 1000;
        while(true){
            Page<PointsRecord> iPage = new Page<>(pageNo, pageSize);
            Page<PointsRecord> pointsRecordPage = this.pointsRecordService.page(iPage);
            if (ObjectUtil.isEmpty(pointsRecordPage.getRecords())){
                break;
            }
            //3、更换数据库名字
            TableInfoContext.setInfo(POINTS_BOARD_RECORD_TABLE_PREFIX + season);
            //4、把数据插入到新表
            this.pointsRecordService.saveBatch(pointsRecordPage.getRecords());
            //5、再恢复到原来的表中
            TableInfoContext.remove();
            //6、翻页
            pageNo+=shardTotal;
        }
    }

    @XxlJob("deletePointsBoard")
    public void deletePointsBoard(){
        List<PointsRecord> pointsRecordList = this.pointsRecordService.list();
        Set<Long> ids = CollStreamUtil.toSet(pointsRecordList, PointsRecord::getId);
        this.pointsRecordService.removeByIds(ids);
    }

}
