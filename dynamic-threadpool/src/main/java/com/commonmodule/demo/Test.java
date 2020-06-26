package com.commonmodule.demo;

import com.commonmodule.dynamicthreadpool.DynamicThreadPoolBuilder;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

public class Test {

    public static void main(String[] args) throws InterruptedException {

        ThreadPoolExecutor poolExecutor = DynamicThreadPoolBuilder.custom().setDefaultCorePoolSize(50).setDefaultMaximumPoolSize(500)
                .setDefaultKeepAliveTime(60000)
                .setDefaultThreadPoolName("custom-pool-")
                .setAutoScale(true).Builder();

        poolExecutor.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("process...");
        });


        for (; ; ) {
            Thread.sleep(1000);
        }


    }
}
