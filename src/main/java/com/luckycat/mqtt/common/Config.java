package com.luckycat.mqtt.common;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * User: fafu
 * Date: 14-5-30
 * Time: 下午1:44
 * This class is
 */
public class Config {
    private static final Logger LOG = LoggerFactory.getLogger(Config.class);
    public static final long NO_CONNECT_MSG_TIMEOUT = 1000;
    public static final String NO_CONNECT_MSG_TIMEOUT_KEY = "no.connect.msg.timeout";

    public static final long NO_DISCONNECT_TIMEOUT = 1000;
    public static final String NO_DISCONNECT_TIMEOUT_KEY = "no.disconnect.timeout";

    public static final String PROTOCAL_NAME_KEY = "mqtt.protocal.name";
    public static final String PROTOCAL_NAME_DEFAULT = "MQIsdp";
    public static final String PROTOCAL_VERSION_KEY = "mqtt.protocal.name";
    public static final byte PROTOCAL_VERSION_DEFAULT = 3;

    private Configuration config;

    public String getProtocalName(){
        return config.getString(PROTOCAL_NAME_KEY,PROTOCAL_NAME_DEFAULT);
    }

    public byte getProtocalVersion(){
        return config.getByte(PROTOCAL_VERSION_KEY,PROTOCAL_VERSION_DEFAULT);
    }

    public long getNoConnectMsgTimeout() {
        return noConnectMsgTimeout;
    }

    public void setNoConnectMsgTimeout(long noDisConnectMsgTimeout) {
        this.noConnectMsgTimeout = noConnectMsgTimeout;
    }

    public long geDisConnectTimeout() {
        return config.getLong(NO_DISCONNECT_TIMEOUT_KEY,NO_DISCONNECT_TIMEOUT);
    }

    public void setNoDisConnectMsgTimeout(long noDisConnectTimeout) {
        this.noDisConnectTimeout = noDisConnectTimeout;
    }

    private long noDisConnectTimeout = NO_DISCONNECT_TIMEOUT;
    private long noConnectMsgTimeout = NO_CONNECT_MSG_TIMEOUT;

    private Config(){

    }

    public Config(String properties){
        try {
            config = new PropertiesConfiguration(properties);
        } catch (ConfigurationException e) {
            config = new PropertiesConfiguration();
        }

    }

    public void init(){
        noConnectMsgTimeout = config.getLong(NO_CONNECT_MSG_TIMEOUT_KEY,NO_CONNECT_MSG_TIMEOUT);
    }
}
