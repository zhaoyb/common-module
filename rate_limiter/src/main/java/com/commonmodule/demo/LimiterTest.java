package com.commonmodule.demo;

import com.commonmodule.ratelimiter.LimitUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LimiterTest {
    public static void main(String[] args) throws InterruptedException {

        LimitUtil.init("127.0.0.1", 6379, null);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(0);
        Executor executor = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(30);
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 30; i++) {
            executor.execute(() -> {
                totalCount.incrementAndGet();
                boolean isAllow = LimitUtil.rateLimit("a", 10, 10);
                if (isAllow) {
                    successCount.incrementAndGet();
                }
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        System.out.println("cost:" + (System.currentTimeMillis() - begin) + "ms,total:" + totalCount.get() + ",success:" + successCount.get());
    }
}
