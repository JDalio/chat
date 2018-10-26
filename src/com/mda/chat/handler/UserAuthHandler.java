package com.mda.chat.handler;

import com.alibaba.fastjson.JSONObject;
import com.mda.chat.proto.ChatCode;
import com.mda.chat.proto.Message;
import com.mda.chat.proto.MessageType;
import com.mda.chat.server.Server;
import com.mda.chat.utils.NettyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAuthHandler extends SimpleChannelInboundHandler<Object>
{
    private static final Logger logger = LoggerFactory.getLogger(UserAuthHandler.class);

    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof FullHttpRequest)
        {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame)
        {
            handleWebSocket(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof IdleStateEvent)
        {
            IdleStateEvent evnet = (IdleStateEvent) evt;
            // 判断Channel是否读空闲, 读空闲时移除Channel
            if (evnet.state().equals(IdleState.READER_IDLE))
            {
                final String remoteAddress = NettyUtil.parseChannelRemoteAddr(ctx.channel());
                logger.warn("ChannelRead Timeout, IDLE exception: [{}]", remoteAddress);
                UserInfoManager.removeChannel(ctx.channel());
                UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request)
    {
        if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade")))
        {
            logger.warn("Protocol don't support WebSocket");
            ctx.channel().close();
            return;
        }

        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(
                Server.PREV_PROTO_URL, null, true);
        handshaker = handshakerFactory.newHandshaker(request);//根据request分析协议，创建handshaker
        if (handshaker == null)
        {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else
        {
            // 动态加入websocket的编解码处理
            handshaker.handshake(ctx.channel(), request);//根据request中Sec-WebSocket-Key构建response中的Sec-WebSocket-Key
            // 存储已经连接的Channel
            UserInfoManager.addChannel(ctx.channel());
        }
    }

    private void handleWebSocket(ChannelHandlerContext ctx, WebSocketFrame frame)
    {
        System.out.println("*****WebSocket Frame*****"+frame);
        // 判断是否关闭链路命令
        if (frame instanceof CloseWebSocketFrame)
        {
            logger.info("*********WebSocket Close**********");
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            UserInfoManager.removeChannel(ctx.channel());
            return;
        }
        // 判断是否Ping消息
        if (frame instanceof PingWebSocketFrame)
        {
            logger.info("*********WebSocket Ping**********");

            logger.info("*****Ping Message init ref: "+frame.refCnt());

            logger.info("ping message:{}", frame.content().retain());

            logger.info("*****Ping Message after logger ref: "+frame.refCnt());

            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));

            logger.info("*****Pong Message after write and flush ref: "+frame.refCnt());

            return;
        }
        // 判断是否Pong消息
        if (frame instanceof PongWebSocketFrame)
        {
            logger.info("*********WebSocket Pong**********");
            logger.info("*****Pong Message init ref: "+frame.refCnt());

            logger.info("pong message:{}", frame.content().retain());

            logger.info("*****Pong Message after logger ref: "+frame.refCnt());

            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));

            logger.info("*****Pong Message after write and flush ref: "+frame.refCnt());

            return;
        }

        // 本程序目前只支持文本消息
        if (!(frame instanceof TextWebSocketFrame))
        {
            throw new UnsupportedOperationException(frame.getClass().getName() + " frame type not supported");
        }

        String message = ((TextWebSocketFrame) frame).text();
        JSONObject json = JSONObject.parseObject(message);
        MessageType type = MessageType.valueOf(json.getInteger("code"));
        Channel channel = ctx.channel();
        switch (type)
        {
            case PING:
                UserInfoManager.updateUserTime(channel);
                UserInfoManager.sendPong(ctx.channel());
                return;
            case PONG:
                UserInfoManager.updateUserTime(channel);
                return;
            case AUTH:
                boolean isSuccess = UserInfoManager.saveUser(channel, json.getString("nick"));
                UserInfoManager.sendInfo(channel, ChatCode.SYS_AUTH_STATE, isSuccess);
                if (isSuccess)
                {
                    UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
                }
                return;
            case MESS: //普通的消息留给MessageHandler处理
                System.out.println("******Before:"+frame.refCnt());
                frame.retain();
                logger.info("*****Receive text message{}", frame.content());
                System.out.println("******After log:"+frame.refCnt());
                break;
            default:
                logger.warn("The code [{}] can't be auth!!!", code);
                return;
        }
        //后续消息交给MessageHandler处理
        ctx.fireChannelRead(frame);
    }
}
