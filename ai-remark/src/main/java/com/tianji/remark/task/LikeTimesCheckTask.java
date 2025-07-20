package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LikeTimesCheckTask {
    private final ILikedRecordService likedRecordService;
    private static final List<String> bizType = List.of("QA","NOTE");
    private static final int MAX_BIZ_SIZE = 30;

    @Scheduled(fixedDelay = 20000)
    public void checkLikeTimes(){
        for (String type : bizType){
            this.likedRecordService.readLikedTimesAndSendMessage(type, MAX_BIZ_SIZE);
        }
    }
}
