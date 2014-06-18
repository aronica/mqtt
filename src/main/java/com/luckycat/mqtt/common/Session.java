package com.luckycat.mqtt.common;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import java.nio.ByteBuffer;

/**
 * User: fafu
 * Date: 14-5-30
 * Time: 下午3:33
 * This class is
 */
public class Session {
    public String clientIdentifier;
    public String username;
    public String password;
    public Connection conn;
    public String willTopic;
    public ByteBuffer willMessage;
    public int keepAliveTimer;
    public boolean cleanSession;

    public Session(String client,String username,String password,Connection conn,String willTopic,ByteBuffer willMessage,
                   int keepAliveTimer,boolean cleanSession){
        this.clientIdentifier = client;
        this.cleanSession = cleanSession;
        this.username = username;
        this.password = password;
        this.conn =  conn;
        this.willMessage = willMessage;
        this.willTopic = willTopic;
        this.keepAliveTimer = keepAliveTimer;
//        this.rb = new ArrayBlockingQueue<Message>(1024);
    }

    public void setIdleTime(int timeout){
         ChannelPipeline pipeline = conn.channel.getPipeline();
        if(pipeline.getNames().contains("idleStateHandler")){
            pipeline.remove("idleStateEvent");
        }
        if(pipeline.getNames().contains("idleEventHandler")){
            pipeline.remove("idleEventHandler");
        }
        pipeline.addFirst("idleStateHandler",new MyIdleStateHandler(new HashedWheelTimer(),0,0,timeout));
//        pipeline.addAfter("idleStateHandler","idleEventHander",new );
    }

    public class MyIdleStateHandler extends IdleStateHandler{

        public MyIdleStateHandler(Timer timer, int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
            super(timer, readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
        }

        @Override
        protected void channelIdle(ChannelHandlerContext ctx, IdleState state, long lastActivityTimeMillis) throws Exception {
            if(state==IdleState.ALL_IDLE||state == IdleState.READER_IDLE){
                ctx.getChannel().disconnect();
                return;
            }
            super.channelIdle(ctx, state, lastActivityTimeMillis);
        }
    }
}
