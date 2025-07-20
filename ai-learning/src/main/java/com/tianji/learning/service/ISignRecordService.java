package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;

import java.util.List;

public interface ISignRecordService {
    SignResultVO addSign();

    List<Integer> selectSign();


}

