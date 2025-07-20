package com.tianji.promotion.utils;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
@Aspect
@RequiredArgsConstructor
public class MyLockAspect implements Ordered {

    private final MyLockFactory lockFactory;
    @Around("@annotation(myLock)")
    public Object tryLock(ProceedingJoinPoint pjp, MyLock myLock) throws Throwable {
        //1、利用我的锁工厂创建锁对象
        RLock lock = lockFactory.getLock(MyLockType.RE_ENTRANT_LOCK, myLock.name());
        //2、尝试获取锁
        boolean isLock = myLock.myLockStrategy().tryLock(lock,myLock);
        //3、判断是否获取成功
        if (!isLock){
            return null;
        }
        try {
            //4、成功执行业务
            return pjp.proceed();
        } finally {
            //5、释放锁
            lock.unlock();
        }
    }
    @Override
    public int getOrder() {
        return 0;
    }
}
