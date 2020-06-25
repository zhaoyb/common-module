package com.commonmodule.demo;

import com.commonmodule.sharptime.SharpTime;

public class Test {

    public static void main(String[] args) {

        long now = System.currentTimeMillis();
        System.out.println(now);
        long nextBatchTime = SharpTime.nextBatchTime(now, 5000);
        System.out.println(nextBatchTime);

    }

}
