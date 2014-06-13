package com.luckycat.mqtt.common;

/**
 * User: fafu
 * Date: 14-6-12
 * Time: 下午5:42
 * This class is
 */
public class PublishEvent {
    public Subscription subscription;
    public Topic topic;

    public PublishEvent(){

    }
    public PublishEvent(Subscription subscription,Topic topic){
        this.subscription = subscription;
        this.topic = topic;
    }
}
