package com.luckycat.mqtt.common;

import org.jboss.netty.channel.*;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: fafu
 * Date: 14-5-30
 * Time: 上午10:50
 * This class is
 */
public class ServerHandler extends SimpleChannelHandler {
    private ArrayBlockingQueue<Connection> connectionQueue = new ArrayBlockingQueue<Connection>(1000000);
    private Context context;

    private ConcurrentHashMap<Channel,Session> sessionMap = new ConcurrentHashMap<Channel,Session>();
    private ConcurrentHashMap<String,Topic> topicMap = new ConcurrentHashMap<String,Topic>();
    private ConcurrentHashMap<String,Set<Topic>> userSubTopicMap = new ConcurrentHashMap<String,Set<Topic>>();
    private ConcurrentHashMap<String,Set<Session>> topicSubMap = new ConcurrentHashMap<String,Set<Session>>();


    private Map<String,String> up = new HashMap<String,String>();
    {
        up.put("ffy","good");
    }

    private Thread connectTimeoutThread = new Thread(new Runnable(){
        @Override
        public void run() {
            try {
                while(true){
                    Connection conn = connectionQueue.take();
                    //Connecting over time.
                    if(conn.status == EnumUtil.Status.CONNECTING){
                        while(true){
                            long sleep = context.getNoConnectMsgTimeout() -
                                    (System.currentTimeMillis()-conn.connectingTime);
                            if(conn.status == EnumUtil.Status.CONNECTING&&sleep <= 0){
                                if(conn.channel.isOpen()){
                                    conn.channel.close();
                                    break;
                                }
                            }
                            Thread.sleep(sleep+10);
                        }


                    }

                }
            } catch (InterruptedException e) {

            }
        }
    });

    public ServerHandler() {
        super();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        super.messageReceived(ctx,e);
        Message message = (Message)e.getMessage();
        Channel channel = ctx.getChannel();
        Connection conn = (Connection)channel.getAttachment();
        Session session = (Session)sessionMap.get(channel);
        if(conn.status == EnumUtil.Status.DISCONNECTING||conn.status == EnumUtil.Status.DISCONNECTED){
            //ignore requests like this since connection status is not alright.
            return;
        }
        switch(message.messageType){
            case Reserved:
                break;
            case CONNECT:
                String clientIdentifier = message.clientIdentifier;
                //old client identifier already exists, then the old session should be disconnected and new session should
                //be create through a new connecting flow.
                if(session != null){
                    session.conn.channel.disconnect();
                    sessionMap.remove(clientIdentifier);
                }

                if(conn != null && conn.status!= EnumUtil.Status.CONNECTING){
                    if(channel.isOpen()){
                        channel.close();
                    }
                    return;
                }
                if(!message.protocalName.equals(context.getProtocalName())||
                        message.protocalVersion != context.getProtocalVersion()){
                    channel.write(MessageFactory.newConnackMessage(EnumUtil.ReturnCode.REFUSED_PROTOCAL_ERROR));
                    channel.close();
                    return;
                }
                if(message.username != null&&message.password != null){
                    String pwd = up.get(message.username);
                    if(pwd == null||!pwd.equals(message.password)){
                        channel.write(MessageFactory.newConnackMessage(EnumUtil.ReturnCode.REFUSED_BAD_USERNAME_PASSWORD));
                        channel.close();
                        return;
                    }
                }
                if(message.willTopic!= null && message.willMessageByte != null){
                    Topic topic = topicMap.get(message.willTopic);
                    if( topic == null){
                        topic = new Topic(message.willTopic);
                        topic = topicMap.putIfAbsent(message.willTopic,topic);
                    }
                    topic.add(message.willMessageByte);
                }
                conn.status = EnumUtil.Status.CONNECTED;
                conn.clearSession = (message.connectFlags&0x01)==1?true:false;
                conn.clientIdentifier = clientIdentifier;
                conn.connectedTime = System.currentTimeMillis();
                session = new Session(message.clientIdentifier,message.username,message.password,conn);
                sessionMap.put(channel,session);
                channel.write(MessageFactory.newConnackMessage(EnumUtil.ReturnCode.ACCEPT));
                break;
            case CONNACK:
                break;
            case PUBLISH:
                if(message.qos== EnumUtil.QoS.ALO){
                    String topicName = message.topicName;
                    int messageId = message.messageId;
                    Topic topic = topicMap.get(message.willTopic);
                    if( topic == null){
                        topic = new Topic(topicName, EnumUtil.QoS.ALO);
                        topic = topicMap.putIfAbsent(topicName,topic);
                    }
                    topic.add(message.payload);
                    Set<Topic> set = new LinkedHashSet<Topic>();
                    set = userSubTopicMap.putIfAbsent(message.clientIdentifier,set);
                    set.add(topic);
                    Message puback = MessageFactory.newPubackMessage(messageId);
                    channel.write(puback);
                    return;
                } else if(message.qos == EnumUtil.QoS.EO){
                    String topicName = message.topicName;
                    int messageId = message.messageId;
                    Topic topic = topicMap.get(topicName);
                    if( topic == null){
                        topic = new Topic(topicName, EnumUtil.QoS.EO);
                        topic = topicMap.putIfAbsent(topicName,topic);
                    }
                    Set<Topic> set = new LinkedHashSet<Topic>();
                    set = userSubTopicMap.putIfAbsent(message.clientIdentifier,set);
                    set.add(topic);
                    topic.add(message.payload);
                    Message puback = MessageFactory.newPubrecMessage(messageId);
                    channel.write(puback);
                    return;
                }
                break;
            case PUBACK:
                break;
            case PUBREC:
                channel.write(MessageFactory.newPubrelMessage(message.messageId));
                break;
            case PUBREL:
                channel.write(MessageFactory.newPubcomMessage(message.messageId));
                return;
            case PUBCOMP:
                break;
            case SUBSCRIBE:
                List<EnumUtil.QoS> qosList = new ArrayList<EnumUtil.QoS>();
                for(int i = 0 ;i<message.topic.length;i++){
                    String topicName = message.topic[i];
                    Topic topic = topicMap.get(topicName);
                    if(topic == null){
                        topic = new Topic(topicName,message.qos);
                        topicMap.putIfAbsent(topicName,topic);
                    }
                    EnumUtil.QoS sourceQos = topic.qos;
                    EnumUtil.QoS subQos = EnumUtil.QoS.values()[message.topicQosLevel[i]];
                    EnumUtil.QoS qosNeed = sourceQos.compareTo(subQos)>0?subQos:sourceQos;
                    qosList.add(qosNeed);
                }
                channel.write(MessageFactory.newSubackMessage(message.messageId, qosList));
                break;
            case SUBACK:
                break;
            case UNSUBSCRIBE:
                int messageId = message.messageId;
                for(int i = 0 ;i<message.topic.length;i++){
                    String topicName = message.topic[i];
                    Topic topic = topicMap.get(topicName);
                    if(topic == null){
                        topic = new Topic(topicName,message.qos);
                        topicMap.putIfAbsent(topicName,topic);
                    }
                    Set<Topic> topics = userSubTopicMap.get(topicName);
                    topics.remove(topic);
                    Set<Session> sessions =  topicSubMap.get(topicName);
                    sessions.remove(session);
                }
                channel.write(MessageFactory.newUnsubackMessage(messageId)) ;
                break;
            case UNSUBACK:
                break;
            case PINGREQ:
                channel.write(MessageFactory.newPinrespMessage());
                break;
            case PINGRESP:
                //Ignore this situation since pingresp will never received by the server.
                break;
            case DISCONNECT:
                conn.status = EnumUtil.Status.DISCONNECTING;
                if(conn.clearSession){
                    sessionMap.remove(conn.clientIdentifier);
                }
                //channel.close();
                conn.disconnectingTime = System.currentTimeMillis();
                break;
            case Reserved2:
                break;
            default:
                ;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        super.exceptionCaught(ctx, e);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelConnected(ctx, e);
        Connection conn = ConnectionFactory.newConnection(ctx.getChannel());
        connectionQueue.add(conn);
        Channel channel = ctx.getChannel();
        channel.setAttachment(conn);
    }
}
