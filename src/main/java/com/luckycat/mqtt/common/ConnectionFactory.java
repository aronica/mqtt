package com.luckycat.mqtt.common;

import org.jboss.netty.channel.Channel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: fafu
 * Date: 14-5-30
 * Time: 下午2:50
 * This class is
 */
public class ConnectionFactory {

    private static AtomicInteger ider = new AtomicInteger();

    public static Connection newConnection(Channel channel){
        Connection conn = new Connection();
        conn.id = ider.getAndIncrement();
        conn.connectingTime = System.currentTimeMillis();
        conn.status = EnumUtil.Status.CONNECTING;
        return conn;
    }
}
