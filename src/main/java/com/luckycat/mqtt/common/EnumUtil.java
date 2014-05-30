package com.luckycat.mqtt.common;

/**
 * User: fafu
 * Date: 14-5-26
 * Time: 下午2:09
 * This class is
 */
public class EnumUtil {

    public static enum MessageType{
            Reserved,CONNECT,CONNACK,PUBLISH,PUBACK,PUBREC,PUBREL,PUBCOMP,SUBSCRIBE
                    ,SUBACK,UNSUBSCRIBE,UNSUBACK,PINGREQ,PINGRESP,DISCONNECT,Reserved2
    }

    public static enum QoS{
        AMO,ALO,EO,R
    }
}
