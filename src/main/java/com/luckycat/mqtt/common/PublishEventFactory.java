package com.luckycat.mqtt.common;

import com.lmax.disruptor.EventFactory;

/**
 * User: fafu
 * Date: 14-6-13
 * Time: 上午10:33
 * This class is
 */
public class PublishEventFactory implements EventFactory<Event> {
    @Override
    public Event newInstance() {
        return new Event();
    }
}
