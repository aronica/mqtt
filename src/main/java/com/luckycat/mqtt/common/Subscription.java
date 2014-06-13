package com.luckycat.mqtt.common;

/**
 * User: fafu
 * Date: 14-6-12
 * Time: 下午1:04
 * This class is
 */
public class Subscription {
    public String key;
    public Session session;
    public long subTime;

    public Subscription(String key,Session session){
        this.key = key;
        this.session = session;
        this.subTime = System.currentTimeMillis();
    }

}
