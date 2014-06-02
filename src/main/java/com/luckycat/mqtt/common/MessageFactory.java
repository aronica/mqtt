package com.luckycat.mqtt.common;
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
}
