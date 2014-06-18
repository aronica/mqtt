package com.luckycat.mqtt.common;
import java.io.UnsupportedEncodingException;
import java.util.List;

import static com.luckycat.mqtt.common.EnumUtil.*;
/**
 * User: fafu
 * Date: 14-5-30
 * Time: 下午4:12
 * This class is
 */
public class MessageFactory {

    public static Message newConnackMessage(ReturnCode returnCode){
        Message message = new Message();
        message.messageType = MessageType.CONNACK;
        message.remainLength = 2;
        message.variableHeader = new byte[]{0,returnCode.code()};
        return message;
    }

    public static Message newPubackMessage(int messageId) {
        Message message = new Message();
        message.messageType = MessageType.PUBACK;
        message.remainLength = 2;
        message.variableHeader = new byte[]{ (byte)(messageId>>8),(byte)(messageId&0xFF)};
        return message;
    }

    public static Message newPubrecMessage(int messageId) {
        Message message = new Message();
        message.messageType = MessageType.PUBREC;
        message.remainLength = 2;
        message.variableHeader = new byte[]{ (byte)(messageId>>8),(byte)(messageId&0xFF)};
        return message;
    }

    public static Message newPubrelMessage(int messageId) {
        Message message = new Message();
        message.messageType = MessageType.PUBREL;
        message.remainLength = 2;
        message.variableHeader = new byte[]{ (byte)(messageId>>8),(byte)(messageId&0xFF)};
        return message;
    }

    public static Message newPubcomMessage(int messageId) {
        Message message = new Message();
        message.messageType = MessageType.PUBREL;
        message.remainLength = 2;
        message.variableHeader = new byte[]{ (byte)(messageId>>8),(byte)(messageId&0xFF)};
        return message;
    }

    public static Message newSubackMessage(int messageId, List<QoS> qosList) {
        Message message = new Message();
        message.messageType = MessageType.SUBACK;
        message.remainLength = 2+qosList.size();
        message.variableHeader = new byte[]{ (byte)(messageId>>8),(byte)(messageId&0xFF)};
        message.payload = new byte[qosList.size()];
        for(int i = 0;i<qosList.size();i++)   {
            message.payload[i] = (byte)qosList.get(i).ordinal() ;
        }
        return message;
    }

    public static Object newUnsubackMessage(int messageId) {
        Message message = new Message();
        message.messageType = MessageType.UNSUBACK;
        message.remainLength = 2;
        message.variableHeader = new byte[]{ (byte)(messageId>>8),(byte)(messageId&0xFF)};
        return message;

    }

    public static Object newPinrespMessage() {
        Message message = new Message();
        message.messageType = MessageType.PINGRESP;
        message.remainLength = 0;
        return message;
    }

    public static Message newWillPublishMessage(Message m2) {
        Message message = new Message();
        message.messageType = MessageType.PUBLISH;
        message.qos = EnumUtil.QoS.values()[((m2.connectFlags>>3)&0x03)];
        message.clientIdentifier = m2.clientIdentifier;
        message.topicName = m2.willTopic;
        message.messageId = 1;
        message.info = m2.willMessageByte.array();
        message.Retain = (byte)((m2.connectFlags>>5)&0x01);
        try {
            message.remainLength = message.topicName.getBytes("UTF-8").length+
                    ((message.qos== QoS.ALO||message.qos==QoS.EO)?2:0)+
            message.info.length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return message;
    }
}
