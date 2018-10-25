package com.wolfbe.chat;

import com.wolfbe.chat.server.ChatServer;
import com.wolfbe.chat.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args)
    {
        int port=Server.DEFAULT_PORT;
        if (args.length > 0)
        {
            try
            {
                port=Integer.valueOf(args[0]);
            }
            catch (NumberFormatException e)
            {
                e.printStackTrace();
            }
        }

        final ChatServer server = new ChatServer(port);
        server.start();

        // 注册进程钩子，在JVM进程关闭前释放资源
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                server.shutdown();
                logger.warn(">>>>>>>>>> jvm shutdown");
                System.exit(0);
            }
        });
    }
}
