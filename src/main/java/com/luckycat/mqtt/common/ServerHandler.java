package com.luckycat.mqtt.common;

import org.jboss.netty.channel.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    private ConcurrentHashMap<String,Session> sessionMap = new ConcurrentHashMap<String,Session>();
    private ConcurrentHashMap<String,Topic> topicMap = new ConcurrentHashMap<String,Topic>();
    private ConcurrentHashMap<String,Set<Topic>> userSubTopicMap = new ConcurrentHashMap<String,Set<Topic>>();



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
                    if(conn.status != EnumUtil.Status.CONNECTING){
                        continue;
                    }
                    long sleep = context.getNoConnectMsgTimeout() -
                            (System.currentTimeMillis()-conn.connectingTime);
                    if(conn.status == EnumUtil.Status.CONNECTING&&sleep <= 0){
                        if(conn.channel.isOpen()){
                            conn.channel.close();
                        }
                    }
                    Thread.sleep(sleep);
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
        switch(message.messageType){
            case Reserved:
                break;
            case CONNECT:
                String clientIdentifier = message.clientIdentifier;
                Session session = sessionMap.get(clientIdentifier);
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
                conn.connectedTime = System.currentTimeMillis();
                session = new Session(message.clientIdentifier,message.username,message.password,conn);
                sessionMap.put(message.clientIdentifier,session);
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
                        topic = new Topic(message.willTopic);
                        topic = topicMap.putIfAbsent(message.willTopic,topic);
                    }
                    topic.add(message.willMessageByte);
                }
                break;
            case PUBACK:
                break;
            case PUBREC:
                break;
            case PUBREL:
                break;
            case PUBCOMP:
                break;
            case SUBSCRIBE:
                break;
            case SUBACK:
                break;
            case UNSUBSCRIBE:
                break;
            case UNSUBACK:
                break;
            case PINGREQ:
                break;
            case PINGRESP:
                break;
            case DISCONNECT:
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
