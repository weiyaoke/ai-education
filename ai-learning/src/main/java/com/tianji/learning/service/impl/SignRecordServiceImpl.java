package com.tianji.learning.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.tianji.common.constants.MqConstants.Exchange.LEARNING_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.SIGN_IN;
import static com.tianji.learning.constants.RedisConstants.SIGN_RECORD_KEY_PREFIX;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;
    /**
     * 添加签到记录
     */
    @Override
    public SignResultVO addSign() {
        //1、获取当前用户
        Long userId = UserContext.getUser();
        //2、使用bitmap来添加用户的签到记录
        LocalDate nowTime = LocalDate.now();
        String key = SIGN_RECORD_KEY_PREFIX + userId + nowTime.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        Boolean bool = this.redisTemplate.opsForValue().setBit(key, nowTime.getDayOfMonth() - 1, true);
        if (BooleanUtil.isTrue(bool)){
            throw new BizIllegalException("不允许重复签到");
        }
        //3、查询连续签到的天数
        Integer count = this.countSignDays(key,nowTime.getDayOfMonth());
        //4、连续签到获得的分数奖励
        int points = 1;
        switch (count){
            case 7:
                points = points + 10;
                break;
            case 14:
                points = points + 20;
                break;
            case 28:
                points = points + 40;
                break;
            default:
                break;
        }
        //4.1发消息给mq更新用户积分数据
        this.mqHelper.send(LEARNING_EXCHANGE,SIGN_IN,SignInMessage.of(userId,points));
        //5、封装返回
        SignResultVO signResultVO = new SignResultVO();
        signResultVO.setSignDays(count);
        signResultVO.setSignPoints(points);
        return signResultVO;
    }

    /**
     * 查询用户本月到现在的签到记录
     * @return
     */
    @Override
    public List<Integer> selectSign() {
        //1、获取用户id
        Long userId = UserContext.getUser();
        //2、从redis中取出签到记录信息
        LocalDate nowTime = LocalDate.now();
        String key = SIGN_RECORD_KEY_PREFIX + userId + nowTime.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        int days = nowTime.getDayOfMonth();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < days ; i++){
            Boolean b = this.redisTemplate.opsForValue().getBit(key, i);
            if (BooleanUtil.isTrue(b)){
                list.add(1);
            }else {
                list.add(0);
            }
        }
        return list;
    }

    private Integer countSignDays(String key, int dayOfMonth) {
        // 1.获取本月从第一天开始，到今天为止的所有签到记录
        List<Long> longList = this.redisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (ObjectUtil.isEmpty(longList)){
            return 0;
        }
        Long days = longList.get(0);
        // 2.定义一个计数器
        Integer count = 0;
        // 3.循环，与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while((days & 1) == 1){
            // 4.计数器+1
            count++;
            // 5.把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            days >>>= 1;
        }
        return count;
    }
}
