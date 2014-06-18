package com.luckycat.mqtt.common;

import org.jboss.netty.channel.Channel;

import static com.luckycat.mqtt.common.EnumUtil.Status;
/**
 * User: fafu
 * Date: 14-5-30
 * Time: 下午2:20
 * This class is
 */
public class Connection {

    public volatile Status status;
    public long connectingTime;
    public long connectedTime;
    public long id;
    public Channel channel;
    public boolean clearSession;
    public String clientIdentifier;
    public volatile long disconnectingTime;
    public volatile boolean disconnecting;

    public Connection(){

    }



}
