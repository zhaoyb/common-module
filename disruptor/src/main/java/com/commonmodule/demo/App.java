package com.commonmodule.demo;


import com.commonmodule.disruptor.EventData;
import com.commonmodule.disruptor.EventPublisher;

public class App {
    public static void main(String[] args) {

        EventPublisher eventPublisher = new EventPublisher();


        eventPublisher.init();

        eventPublisher.publishEvent(new EventData("a"));

        eventPublisher.close();
    }
}
