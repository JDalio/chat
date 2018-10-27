package com.mda.chat.server;

import com.mda.chat.handler.MessageHandler;
import com.mda.chat.handler.AuthHandler;
import com.mda.chat.handler.UserInfoManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer implements Server
{

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private int port;

    private DefaultEventLoopGroup defLoopGroup;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workGroup;
    private ServerBootstrap b;

    private ScheduledExecutorService executorService;

    public ChatServer(int port)
    {
        this.port=port;

        //default: use 8 threads to listen 8 ports
        defLoopGroup = new DefaultEventLoopGroup(8, new ThreadFactory()
        {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r)
            {
                return new Thread(r, "DEFAULTEVENTLOOPGROUP_" + index.incrementAndGet());
            }
        });

        //listen the connect, give the socket to workers
        bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new ThreadFactory()
        {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r)
            {
                return new Thread(r, "BOSS_" + index.incrementAndGet());
            }
        });

        workGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 10, new ThreadFactory()
        {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r)
            {
                return new Thread(r, "WORK_" + index.incrementAndGet());
            }
        });

        b = new ServerBootstrap();
        b.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>()
                {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception
                    {
                        ch.pipeline().addLast(defLoopGroup,
                                new HttpServerCodec(),   //请求解码器
                                new HttpObjectAggregator(65536),//将多个消息转换成单一的消息对象
                                new ChunkedWriteHandler(),  //支持异步发送大的码流，一般用于发送文件流
                                new IdleStateHandler(60, 0, 0), //检测链路是否读空闲
                                new AuthHandler(), //处理握手和认证
                                new MessageHandler()    //处理消息的发送
                        );
                    }
                });

        executorService= Executors.newScheduledThreadPool(2);
    }

    @Override
    public void start()
    {

        try
        {
            //start the server
            ChannelFuture cf = b.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) cf.channel().localAddress();
            logger.info("WebSocketServer start success, port is:{}", addr.getPort());

            // 定时扫描所有的Channel，关闭失效的Channel
            executorService.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    logger.info("scanNotActiveChannel --------");
                    UserInfoManager.scanNotActiveChannel();
                }
            }, 3, 60, TimeUnit.SECONDS);

            // 定时向所有客户端发送Ping消息
            executorService.scheduleAtFixedRate(new Runnable()
            {
                @Override
                public void run()
                {
                    UserInfoManager.broadCastPing();
                }
            }, 3, 50, TimeUnit.SECONDS);

        }
        catch (InterruptedException e)
        {
            logger.error("WebSocketServer start fail,", e);
        }
    }

    @Override
    public void shutdown()
    {
        if (executorService != null)
        {
            executorService.shutdown();
        }

        if (defLoopGroup != null)
        {
            defLoopGroup.shutdownGracefully();
        }
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }
}
