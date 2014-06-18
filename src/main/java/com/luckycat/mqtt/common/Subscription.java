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
    public EnumUtil.QoS qos;

    public Subscription(String key,Session session,EnumUtil.QoS qos){
        this.key = key;
        this.session = session;
        this.subTime = System.currentTimeMillis();
        this.qos = qos;
    }

}
