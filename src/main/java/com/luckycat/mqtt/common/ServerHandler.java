package com.luckycat.mqtt.common;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import io.netty.util.internal.chmv8.ConcurrentHashMapV8;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * User: fafu
 * Date: 14-5-30
 * Time: 上午10:50
 * This class is
 */
public class ServerHandler extends IdleStateAwareChannelHandler {
    private ArrayBlockingQueue<Connection> connectionQueue = new ArrayBlockingQueue<Connection>(1000000);
    private Context context;

    private ConcurrentHashMap<Channel,Session> sessionMap = new ConcurrentHashMap<Channel,Session>();
//    private ConcurrentHashMap<String,Topic> topicMap = new ConcurrentHashMap<String,Topic>();
    private Map<Session,Set<Subscription>> userSubTopicMap = new HashMap<Session,Set<Subscription>>();
    private ConcurrentHashMap<String,Set<Session>> topicSubMap = new ConcurrentHashMap<String,Set<Session>>();
    private TrieTreeMap<Subscription> topicMap = new TrieTreeMap<Subscription>();
    private ConcurrentHashMapV8<String,Message> messageMap = new ConcurrentHashMapV8<String, Message>();

    Executor executor = Executors.newCachedThreadPool();

    PublishEventFactory factory = new PublishEventFactory();

    int bufferSize = 1024;

    Disruptor<Event> disruptor = new Disruptor<Event>(factory, bufferSize, executor);
    RingBuffer<Event> ringBuffer = disruptor.getRingBuffer();

    private Map<String,String> up = new HashMap<String,String>();
    {
        up.put("ffy","good");
    }

    PublishEventProducer producer = new PublishEventProducer(ringBuffer);

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
        // Connect the handler
        MqttEventHandler handler = new MqttEventHandler();
        disruptor.handleEventsWith(handler)  ;

        // Start the Disruptor, starts all threads running
        disruptor.start();
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
        super.channelIdle(ctx, e);
        if (e.getState() == IdleState.READER_IDLE) {
            Channel channel = ctx.getChannel();
            Session session = (Session)sessionMap.get(channel);
            if(session.willTopic!= null&&session.willMessage!=null){

            }
            e.getChannel().close();
        }
    }

    private void processPublishEvent(Message message,Session session){
        if(message.qos== EnumUtil.QoS.AMO){
            //do nothing for qos amo
            Set<Subscription> subs = topicMap.gets(message.topicName);
            EnumUtil.QoS sourceQos = message.qos;
            for(Subscription sub:subs){
                message.qos = sourceQos.compareTo(sub.qos)>0?sub.qos:sourceQos;
                sub.session.conn.channel.write(message);
            }
        }else if(message.qos== EnumUtil.QoS.ALO){
            Message puback = MessageFactory.newPubackMessage(message.messageId);
            session.conn.channel.write(puback);
            Set<Subscription> subs = topicMap.gets(message.topicName);
            EnumUtil.QoS sourceQos = message.qos;
            for(Subscription sub:subs){
                message.qos = sourceQos.compareTo(sub.qos)>0?sub.qos:sourceQos;
                sub.session.conn.channel.write(message);
            }
            return;
        } else if(message.qos == EnumUtil.QoS.EO){
            Message pubrec = MessageFactory.newPubrecMessage(message.messageId);
            session.conn.channel.write(pubrec);
            messageMap.put(session.clientIdentifier+"_"+message.messageId,message);
            return;
        }
    }

    public class MqttEventHandler implements EventHandler<Event> {
        @Override
        public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println(sequence+","+endOfBatch);
            Message message = event.message;
            Channel channel = event.session.conn.channel;
            switch(event.message.messageType){
                case PUBLISH:
                    processPublishEvent(message,event.session);
                    break;
                case PUBACK:
                    break;
                case PUBREC:
                    Message pubrec = MessageFactory.newPubrecMessage(message.messageId);
                    channel.write(pubrec);
                    return;
                case PUBREL:
                    channel.write(MessageFactory.newPubcomMessage(message.messageId));
                    EnumUtil.QoS sourceQos = message.qos;
                    Set<Subscription> subs = topicMap.gets(event.message.topicName);
                    for(Subscription sub:subs){
                        message.qos = sourceQos.compareTo(sub.qos)>0?sub.qos:sourceQos;
                        sub.session.conn.channel.write(message);
                    }
                    messageMap.remove(event.session.clientIdentifier+"_"+message.messageId);
                    break;
                case DISCONNECT:
                    event.session.conn.status = EnumUtil.Status.DISCONNECTING;
                    if(event.session.conn.clearSession){
                        sessionMap.remove(event.session.conn.clientIdentifier);
                    }
                    //channel.close();
                    event.session.conn.disconnectingTime = System.currentTimeMillis();
                    break;
                default:;
            }

        }
    }

    public class PublishEventProducer{
        private final RingBuffer<Event> ringBuffer;

        public PublishEventProducer(RingBuffer<Event> ringBuffer)
        {
            this.ringBuffer = ringBuffer;
        }

        public void onData(Message message,Session session)
        {
            long sequence = ringBuffer.next();
            try
            {
                Event event = ringBuffer.get(sequence);
                event.session = session;
                event.message = message;
            }
            finally
            {
                ringBuffer.publish(sequence);
            }
        }
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
            case CONNECT:
                String clientIdentifier = message.clientIdentifier;
                //old client identifier already exists, then the old session should be disconnected and new session should
                //be created through a new connecting flow.
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
                    ByteBuffer bb = message.willMessageByte.duplicate();
                    bb.mark();
                    char length = bb.getChar();
                    byte[] b = new byte[length];
                    bb.get(b);
                    for(int i = 0; i<length;i++){
                        if(b[i]<0){
                            //TODO will message content should be character less than 0x7F
                            channel.write(MessageFactory.newConnackMessage(EnumUtil.ReturnCode.REFUSED_PROTOCAL_ERROR));
                            channel.close();
                            return;
                        }
                    }
                    bb.reset();
                    Message m2 = MessageFactory.newWillPublishMessage(message);
                    processPublishEvent(m2,session);
                    //Subscription sub = new Subscription(message.willTopic);
                   // topic.add(message.willMessageByte);
                }
                conn.status = EnumUtil.Status.CONNECTED;
                conn.clearSession = (message.connectFlags&0x01)==1?true:false;
                conn.clientIdentifier = clientIdentifier;
                conn.connectedTime = System.currentTimeMillis();
                boolean cleanSession = (message.connectFlags>1)&0x01==1?true:false;
                session = new Session(message.clientIdentifier,message.username,
                        message.password,conn,message.willTopic,message.willMessageByte,
                        message.keepAliveTimer,cleanSession);
                //ctx.getChannel().getPipeline()
                session.setIdleTime((int)Math.round(message.keepAliveTimer*1.5));
                sessionMap.put(channel,session);
                channel.write(MessageFactory.newConnackMessage(EnumUtil.ReturnCode.ACCEPT));
                break;
            case PUBLISH:
            case PUBREC:
            case PUBREL:
                producer.onData(message, session);
                return;
            case SUBSCRIBE:
                List<EnumUtil.QoS> qosList = new ArrayList<EnumUtil.QoS>();
                for(int i = 0 ;i<message.topic.length;i++){
                    String topicName = message.topic[i];
                    Subscription sub = new Subscription(topicName,session,EnumUtil.QoS.values()[((int)(message.topicQosLevel[i]))]);
                    topicMap.subscribe(topicName,sub);
                }
                channel.write(MessageFactory.newSubackMessage(message.messageId, qosList));
                break;
            case UNSUBSCRIBE:
                int messageId = message.messageId;
                for(int i = 0 ;i<message.topic.length;i++){
                    String topicName = message.topic[i];
                    Subscription sub = new Subscription(topicName,session,EnumUtil.QoS.values()[((int)(message.topicQosLevel[i]))]);
                    topicMap.unsubscribe(topicName, sub);
                }
                channel.write(MessageFactory.newUnsubackMessage(messageId)) ;
                break;
            case PINGREQ:
                channel.write(MessageFactory.newPinrespMessage());
                break;
            case DISCONNECT:
                if(session.cleanSession){
                    Set<Subscription> sub =  userSubTopicMap.get(session);
                    if(sub!=null&&sub.size()>0){
                        for(Subscription s:sub)
                        topicMap.unsubscribe(s.key,s);
                    }
                }
                Map map = (Map)channel.getAttachment();
                map.put("status","disconnecting");
//                channel.disconnect();
                break;
            default:
                ;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        super.exceptionCaught(ctx, e);
        ctx.getChannel().disconnect();
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelConnected(ctx, e);
        Connection conn = ConnectionFactory.newConnection(ctx.getChannel());
        Map map = new HashMap();
        map.put("connection",conn);
        connectionQueue.add(conn);
        Channel channel = ctx.getChannel();
        channel.setAttachment(map);
    }
}
