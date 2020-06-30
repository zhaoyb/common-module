package com.commonmodule.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

public class EventDataHandler implements WorkHandler<EventDataWapper>, EventHandler<EventDataWapper> {

    private String name;

    public EventDataHandler() {
    }


    public EventDataHandler(String name) {
        this.name = name;
    }

    @Override
    public void onEvent(EventDataWapper eventDataWapper) throws Exception {
        System.out.println(name + "-" + ((EventData)eventDataWapper.getData()).getName());
    }

    @Override
    public void onEvent(EventDataWapper eventDataWapper, long l, boolean b) throws Exception {
        System.out.println(name + "-" + ((EventData)eventDataWapper.getData()).getName());
    }
}
