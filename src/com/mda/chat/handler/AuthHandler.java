package com.mda.chat.handler;

import com.alibaba.fastjson.JSONObject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthHandler extends SimpleChannelInboundHandler<Object>
{
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);

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
                UserInfoManager.broadCastInfo(MessageType.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request)
    {
        System.out.println("http request");

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
        // 判断是否关闭链路命令
        if (frame instanceof CloseWebSocketFrame)
        {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            UserInfoManager.removeChannel(ctx.channel());
            return;
        }
        // 本程序目前只支持文本消息
        if (!(frame instanceof TextWebSocketFrame))
        {
            throw new UnsupportedOperationException(frame.getClass().getName() + " frame type not supported");
        }

        String message = ((TextWebSocketFrame) frame).text();
        JSONObject json = JSONObject.parseObject(message);
        int type = json.getInteger("code");
        Channel channel = ctx.channel();
        switch (type)
        {
            case MessageType.PONG:
                UserInfoManager.updateUserTime(channel);
                break;
            case MessageType.AUTH:
                boolean isSuccess = UserInfoManager.saveUser(channel, json.getString("nick"));
                UserInfoManager.sendInfo(channel, MessageType.SYS_AUTH_STATE, isSuccess);
                if (isSuccess)
                {
                    UserInfoManager.broadCastInfo(MessageType.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
                }
                break;
            case MessageType.MESS: //普通的消息留给MessageHandler处理
                ctx.fireChannelRead(frame.retain());
                break;
            default:
                logger.warn("The code [{}] can't be auth!!!", type);
        }

    }
}
