package com.commonmodule.disruptor;

public class EventDataWapper<T> {

    private T data;

    public void setData(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
