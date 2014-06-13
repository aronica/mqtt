package com.luckycat.mqtt.common;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * User: fafu
 * Date: 14-5-26
 * Time: 2:53
 * This class is
 */
public class ServerDecoder extends ReplayingDecoder<ServerDecoder.Phase> {
    public static final ChannelLocal<Message> data = new ChannelLocal<Message>();

    public enum Phase{
        READ_INIT,READ_FIX_HEAD,READ_VARIABLE_HEAD,READ_PAYLOAD
    }

    public ServerDecoder(){
        super(Phase.READ_FIX_HEAD);
    }

    private int decodeRemainLength(ChannelBuffer cb){
        int mul = 1;
        int value = 0;
        byte digit = 0;
        do{
            digit = cb.readByte();
            value += (digit & 0x07F) * mul;
            mul *= 128;
        } while((digit&128) != 0);
        return value;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, Phase state) throws Exception {
        Message message = data.get(channel);
        if(message == null) message = new Message();
        data.set(channel,message);
        switch(state){
            case READ_FIX_HEAD:
                byte[] fixedHeader = new byte[4];
                buffer.readBytes(fixedHeader);
                message.messageType = EnumUtil.MessageType.values()[(int)fixedHeader[0]>>4];
                message.DUP = (byte)(fixedHeader[0]>>3&0x1);
                message.qos = EnumUtil.QoS.values()[(byte)(fixedHeader[0]>>1&0x3)];
                message.remainLength = decodeRemainLength(buffer);
                data.set(channel,message);
                checkpoint(Phase.READ_VARIABLE_HEAD);
            case READ_VARIABLE_HEAD:
                if(message.messageType == EnumUtil.MessageType.CONNECT){
                    buffer.markReaderIndex();
                    int length = buffer.readUnsignedShort();
                    buffer.resetReaderIndex();
                    ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);
                    message.protocalName = in.readUTF();
                    message.protocalVersion = in.readByte();
                    message.connectFlags = in.readByte();
                    message.keepAliveTimer = new byte[2];
                    in.read( message.keepAliveTimer);
                    message.variableHeaderLength = length+5;
                }else if(message.messageType == EnumUtil.MessageType.CONNACK) {
                    message.returnCode = buffer.readByte();
                    message.variableHeaderLength = 1;
                } else if(message.messageType == EnumUtil.MessageType.PUBLISH){
                    buffer.markReaderIndex();
                    int length = buffer.readUnsignedShort();
                    buffer.resetReaderIndex();
                    ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);
                    message.topicName = in.readUTF();
                    if(message.qos == EnumUtil.QoS.ALO||message.qos == EnumUtil.QoS.EO){
                        message.messageId = buffer.readUnsignedShort();
                        length += 2;
                    }
                    message.variableHeaderLength = length;
                }else if(message.messageType == EnumUtil.MessageType.PUBACK||
                        message.messageType == EnumUtil.MessageType.PUBREC ||
                        message.messageType == EnumUtil.MessageType.PUBREL ||
                        message.messageType == EnumUtil.MessageType.PUBCOMP||
                        message.messageType == EnumUtil.MessageType.SUBACK||
                        message.messageType == EnumUtil.MessageType.UNSUBSCRIBE||
                        message.messageType == EnumUtil.MessageType.UNSUBACK ){
                    message.messageId = buffer.readUnsignedShort();
                    message.variableHeaderLength = 2;
                }else if(message.messageType == EnumUtil.MessageType.SUBSCRIBE ){
                    message.messageId = buffer.readUnsignedShort();
                    message.variableHeaderLength = 2;
                }
                data.set(channel,message);
                checkpoint(Phase.READ_PAYLOAD);
            case READ_PAYLOAD:
                if(message.messageType == EnumUtil.MessageType.CONNECT){
                    ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);
                    buffer.markReaderIndex();
                    int length = buffer.readUnsignedShort()+2;
                    buffer.resetReaderIndex();
                    message.clientIdentifier = in.readUTF();
                    if((message.connectFlags&0x02) == 0x02){
                        buffer.markReaderIndex();
                        length+=buffer.readUnsignedShort()+2;
                        buffer.resetReaderIndex();
                        message.willTopic = in.readUTF();
                      //  buffer.markReaderIndex();
                        int byteLen = buffer.readUnsignedShort();
                        length+=byteLen+2;

                        byte[] b = new byte[byteLen];
                        in.read(b);
                        message.willMessageByte = ByteBuffer.wrap(b);
                       // buffer.resetReaderIndex();
//                        message.willMessage = in.readUTF();

                    }

                    //for compatibility with mqttv3 the Remaining Length field from the fixed header takes precedence over the User Name flag.
                    // Server implementations must allow for the possibility that the
                    // User Name flag is set, but the User Name string is missing. This is valid, and connections should be allowed to continue.
                    if((message.connectFlags>>7) == 0x01&&message.remainLength-message.variableHeaderLength-length>0){
                        buffer.markReaderIndex();
                        length+=buffer.readUnsignedShort()+2;
                        buffer.resetReaderIndex();
                        message.username = in.readUTF();
                    }
                    if(((message.connectFlags<<1)>>7) == 0x01&&message.remainLength-message.variableHeaderLength-length>0){
                        message.password = in.readUTF();
                    }
                } else if(message.messageType == EnumUtil.MessageType.PUBLISH){
                    byte[] infoBa = new byte[message.remainLength-message.variableHeaderLength];
                    buffer.readBytes(infoBa);
                    message.info = infoBa;
                }else if(message.messageType == EnumUtil.MessageType.SUBSCRIBE){
                    int remainLength = message.remainLength - message.variableHeaderLength;
                    byte b[] = new byte[remainLength];
                    buffer.readBytes(b);
                    DataInputStream ds = new DataInputStream(new ByteArrayInputStream(b));
                    int read = 0;
                    List<String> topics = new ArrayList<String>();
                    List<Byte> qoses = new ArrayList<Byte>();
                    while(read<remainLength){
                        buffer.markReaderIndex();
                        int length = buffer.readUnsignedShort();
                        buffer.resetReaderIndex();
                        String topic = ds.readUTF();
                        byte qos = buffer.readByte();
                        read+=length+2+1;
                        topics.add(topic);
                        qoses.add(qos);
                    }
                    if(read>0){
                        throw new IOException("Invalid SUBSCRIBE Payload");
                    }
                    message.topic = topics.toArray(new String[0]);
                    message.topicQosLevel = new byte[message.topic.length];
                    int i = 0;
                    for(Byte bs:qoses){
                        message.topicQosLevel[i] = bs.byteValue();
                    }
                }else if(message.messageType == EnumUtil.MessageType.SUBACK){
                    int remainLength = message.remainLength - message.variableHeaderLength;
                    message.topicQosLevel = new byte[remainLength];
                    buffer.readBytes(message.topicQosLevel);
                }else if(message.messageType == EnumUtil.MessageType.UNSUBSCRIBE) {
                    int remainLength = message.remainLength - message.variableHeaderLength;
                    byte b[] = new byte[remainLength];
                    buffer.readBytes(b);
                    DataInputStream ds = new DataInputStream(new ByteArrayInputStream(b));
                    int read = 0;
                    List<String> topics = new ArrayList<String>();
                    while(read<remainLength){
                        buffer.markReaderIndex();
                        int length = buffer.readUnsignedShort();
                        buffer.resetReaderIndex();
                        String topic = ds.readUTF();
                        read+=length+2;
                        topics.add(topic);
                    }
                    if(read>0){
                        throw new IOException("Invalid SUBSCRIBE Payload");
                    }
                    message.topic = topics.toArray(new String[0]);
                }
                data.set(channel,message);
                checkpoint(Phase.READ_FIX_HEAD);
                return message;
            default:
                throw new Error("Shouldn't reach here.");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        super.exceptionCaught(ctx, e);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelConnected(ctx, e);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(ctx, e);
    }
}
