package com.commonmodule.sharptime;

public class SharpTime {
    /**
     * 获取下一个指定间隔的整点时间
     * 例如 当前时刻 :03S 间隔5S，则下一个间隔， 05S  10S , 而不会出现03S  05S
     */
    public static long nextBatchTime(long currentTimeMs, long intervalMs) {
        return currentTimeMs / intervalMs * intervalMs + intervalMs;
    }

}
