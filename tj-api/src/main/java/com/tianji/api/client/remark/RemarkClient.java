package com.tianji.api.client.remark;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;
@FeignClient("remark-service")
public interface RemarkClient {
    @GetMapping("/likes/list")
    Set<Long> selectLikeStatus(@RequestParam("bizIds") Iterable<Long> bizIds);
}
