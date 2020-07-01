package com.commonmodule.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.concurrent.Executors;

public class EventPublisher {

    private Disruptor<EventDataWapper> disruptor;
    private int bufferSize = 1024 * 1024;

    int handlerSize = 2;

    public void init() {
        disruptor = new Disruptor<>(EventDataWapper::new, bufferSize,
                                    Executors.defaultThreadFactory(),
                                    ProducerType.MULTI,
                                    new BlockingWaitStrategy());

        EventDataHandler[] handlers = new EventDataHandler[handlerSize];
        for (int i = 0; i < handlerSize; i++) {
            handlers[i] = new EventDataHandler("handlers(" + i + ")");
        }

        // 点对点， 只有一个消费者能消费到
        //disruptor.handleEventsWithWorkerPool(consumers);
        // 可以使用lambda
//        disruptor.handleEventsWithWorkerPool(eventDataWapper -> {
//            System.out.println(((EventData) eventDataWapper.getData()).getName());
//        });
        // 发布订阅， 所有的消费者 都可以消费到
        disruptor.handleEventsWith(handlers);
        disruptor.setDefaultExceptionHandler(new IgnoreExceptionHandler());
        disruptor.start();
    }

    public void publishEvent(final EventData eventData) {
        final RingBuffer<EventDataWapper> ringBuffer = disruptor.getRingBuffer();
        ringBuffer.publishEvent((event, sequence) -> event.setData(eventData));

    }

    public void close() {
        disruptor.shutdown();
    }

}
