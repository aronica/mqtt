package com.luckycat.mqtt.common;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.jboss.netty.channel.*;

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
public class ServerHandler extends SimpleChannelHandler {
    private ArrayBlockingQueue<Connection> connectionQueue = new ArrayBlockingQueue<Connection>(1000000);
    private Context context;

    private ConcurrentHashMap<Channel,Session> sessionMap = new ConcurrentHashMap<Channel,Session>();
//    private ConcurrentHashMap<String,Topic> topicMap = new ConcurrentHashMap<String,Topic>();
    private ConcurrentHashMap<String,Set<Topic>> userSubTopicMap = new ConcurrentHashMap<String,Set<Topic>>();
    private ConcurrentHashMap<String,Set<Session>> topicSubMap = new ConcurrentHashMap<String,Set<Session>>();
    private TrieTreeMap<Subscription> topicMap = new TrieTreeMap<Subscription>();

    Executor executor = Executors.newCachedThreadPool();

    // The factory for the event
    PublishEventFactory factory = new PublishEventFactory();

    // Specify the size of the ring buffer, must be power of 2.
    int bufferSize = 1024;

    // Construct the Disruptor
    Disruptor<PublishEvent> disruptor = new Disruptor<PublishEvent>(factory, bufferSize, executor);



    // Get the ring buffer from the Disruptor to be used for publishing.
    RingBuffer<PublishEvent> ringBuffer = disruptor.getRingBuffer();

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

    public class MqttEventHandler implements EventHandler<PublishEvent> {
        @Override
        public void onEvent(PublishEvent event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println(sequence+","+endOfBatch);
            Set<Subscription> set = topicMap.gets(event.topic.name);
            for(Subscription sub:set){
                sub.session.conn.channel.write(event.topic.message);
            }
        }
    }

    public class PublishEventProducer{
        private final RingBuffer<PublishEvent> ringBuffer;

        public PublishEventProducer(RingBuffer<PublishEvent> ringBuffer)
        {
            this.ringBuffer = ringBuffer;
        }

        public void onData(Topic topic)
        {
            long sequence = ringBuffer.next();
            try
            {
                PublishEvent event = ringBuffer.get(sequence);
                event.topic = topic;
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
            case Reserved:
                break;
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
                    Topic topic = new Topic(message.willTopic,message);
                    producer.onData(topic);
                   // topic.add(message.willMessageByte);
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

                }else if(message.qos== EnumUtil.QoS.ALO){
                    String topicName = message.topicName;
                    int messageId = message.messageId;
                    Subscription topic = topicMap.get(message.willTopic);

                    if( topic == null){
                        topic = new Subscription(topicName, session);
                        topicMap.put(topicName, topic);
                    }

                    //topic.add(message.payload);
                    Set<Topic> set = new LinkedHashSet<Topic>();
                    set = userSubTopicMap.putIfAbsent(message.clientIdentifier,set);
//                    set.add(topic);
                    Message puback = MessageFactory.newPubackMessage(messageId);
                    channel.write(puback);
                    return;
                } else if(message.qos == EnumUtil.QoS.EO){
                    String topicName = message.topicName;
                    int messageId = message.messageId;
//                    Topic topic = topicMap.get(topicName);
//                    if( topic == null){
//                        topic = new Topic(topicName, EnumUtil.QoS.EO);
//                        topicMap.put(topicName,topic);
//                    }
                    Set<Topic> set = new LinkedHashSet<Topic>();
                    set = userSubTopicMap.putIfAbsent(message.clientIdentifier,set);
//                    set.add(topic);
//                    topic.add(message.payload);
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
                    Set<Subscription> topic = topicMap.gets(topicName);
                    if(topic == null||topic.size()==0){
//                        topic = new Topic(topicName,message.qos);
//                        topicMap.put(topicName, topic);
                    }
//                    EnumUtil.QoS sourceQos = topic.qos;
                    EnumUtil.QoS subQos = EnumUtil.QoS.values()[message.topicQosLevel[i]];
//                    EnumUtil.QoS qosNeed = sourceQos.compareTo(subQos)>0?subQos:sourceQos;
//                    qosList.add(qosNeed);
                }
                channel.write(MessageFactory.newSubackMessage(message.messageId, qosList));
                break;
            case SUBACK:
                break;
            case UNSUBSCRIBE:
                int messageId = message.messageId;
                for(int i = 0 ;i<message.topic.length;i++){
                    String topicName = message.topic[i];
//                    Topic topic = topicMap.get(topicName);
//                    if(topic == null){
//                        topic = new Topic(topicName,message.qos);
//                        topicMap.put(topicName, topic);
//                    }
                    Set<Topic> topics = userSubTopicMap.get(topicName);
                   // topics.remove(topic);
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
