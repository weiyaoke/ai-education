package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.learning.LearningClient;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {

    @Override
    public RemarkClient create(Throwable cause) {
        log.error("查询点赞服务异常", cause);
        return new RemarkClient() {
            @Override
            public Set<Long> selectLikeStatus(Iterable<Long> bizIds) {
                return CollUtils.emptySet();
            }
        };
    }
}
