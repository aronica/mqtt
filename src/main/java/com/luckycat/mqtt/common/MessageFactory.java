package com.luckycat.mqtt.common;
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
}
