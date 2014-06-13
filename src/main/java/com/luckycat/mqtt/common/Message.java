package com.luckycat.mqtt.common;

import java.nio.ByteBuffer;

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
    public byte[] variableHeader;
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
    public byte[] messageIdByte;


    //PUBLISH, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK
    public int identifier;

    /** Payload **/
     public byte[] info;
    //CONNECT
    public String clientIdentifier;
    public String willTopic;
  //  public String willMessage;
    public ByteBuffer willMessageByte;
    public String username;
    public String password;

    //SUBSCRIBE
    public String[] topic;
    public byte[] topicQosLevel;
    public byte[] payload;
}
