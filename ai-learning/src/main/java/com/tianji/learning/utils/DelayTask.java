package com.tianji.learning.utils;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class DelayTask<T> implements Delayed {
    private T data;
    private long deadlineNanos;
    public DelayTask(T data, Duration delayTime) {
        this.data = data;
        this.deadlineNanos = System.nanoTime() + delayTime.toNanos();
    }
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(0, deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS);
    }
    @Override
    public int compareTo(Delayed o) {
        if (this.getDelay(TimeUnit.NANOSECONDS) > o.getDelay(TimeUnit.NANOSECONDS)){
            return 1;
        } else if (this.getDelay(TimeUnit.NANOSECONDS) < o.getDelay(TimeUnit.NANOSECONDS)) {
            return -1;
        }else {
            return 0;
        }
    }
}
