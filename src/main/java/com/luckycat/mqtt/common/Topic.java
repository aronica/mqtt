package com.luckycat.mqtt.common;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * User: fafu
 * Date: 14-5-30
 * Time: 下午4:48
 * This class is
 */
public class Topic {
    public String name;
    public short sendMessageId;
    public short receiverMessageId;
    public Message message;
    public EnumUtil.QoS qos;

    public Topic(String name){this.name = name;}
    public Topic(String name,Message message){
        this.name = name;
        this.message = message;
    }
    public static void main(String[] args){
        System.out.println(Integer.toBinaryString(0x80));
    }

    public ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(Integer.MAX_VALUE);

    public void add(byte[] topic){
        queue.add(topic);
    }
}
