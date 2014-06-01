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

    public static enum ReturnCode{
        ACCEPT((byte)0x00),
        REFUSED_PROTOCAL_ERROR((byte)0x01),
        REFUSED_CLIENT_IDENTIFIER((byte)0x02),
        REFUSED_SERVER_UNAVAILABLE((byte)0x03),
        REFUSED_BAD_USERNAME_PASSWORD((byte)0x04),
        REFUSED_NOT_AUTHORIZED((byte)0x05);
        private byte code;

        ReturnCode(byte code){
            this.code = code;
        }

        public byte code(){
            return code;
        }


    }

    public static enum Status{
        CONNECTING,CONNECTED,SENDING,RECEIVING,DISCONNECTING,DISCONNECTED
    }
}
