package com.commonmodule.disruptor;

import java.io.Serializable;

public class EventData implements Serializable {
    private String name;

    public EventData(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
