package com.luckycat.mqtt.common;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelUpstreamHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;

/**
 * User: fafu
 * Date: 14-6-14
 * Time: 下午9:28
 * This class is
 */
public class MyIdleStateAwareChannelUpstreamHandler extends IdleStateAwareChannelUpstreamHandler {
    public static final ChannelLocal<String> data = new ChannelLocal<String>();

    private Config config;

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
        super.channelIdle(ctx, e);
        long last = e.getLastActivityTimeMillis();
        String status = data.get(ctx.getChannel());
        if(status.equals("connecting")){
            Connection conn = (Connection) ctx.getAttachment();
            if(conn != null&&conn.status == EnumUtil.Status.CONNECTING ){
                if(last - conn.connectingTime>config.getNoConnectMsgTimeout()){
                    ctx.getChannel().disconnect();//No connect msg
                } else{
                    return;//loop again to wait another idle event
                }
            } else{

            }
        }
        ctx.getChannel().disconnect();
    }
}
