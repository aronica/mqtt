package com.luckycat.mqtt.common;

/**
 * User: fafu
 * Date: 14-5-26
 * Time: 下午3:45
 * This class is
 */
public class Message {
    public EnumUtil.MessageType messageType;
    public byte DUP = 0x00;
    public EnumUtil.QoS qos;
    public byte Retain = 0x00;
    public int remainLength;

    public int variableHeaderLength;

    /** variable header**/
    //CONNECT
    public String protocalName;
    public byte protocalVersion;
    public byte connectFlags;
    public byte[] keepAliveTimer;
    //CONNACK
    public byte returnCode;
    //PUBLISH
    public String topicName;
    public int messageId;


    //PUBLISH, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK
    public int identifier;

    /** Payload **/
     public byte[] info;
    //CONNECT
    public String clientIdentifier;
    public String willTopic;
    public String willMessage;
    public String username;
    public String password;

    //SUBSCRIBE
    public String[] topic;
    public byte[] topicQosLevel;
}
