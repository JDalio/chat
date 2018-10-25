package com.wolfbe.chat.server;

public interface Server
{
    String HOST = "localhost";
    int DEFAULT_PORT = 8000;
    String PREV_PROTO_URL = "ws://localhost:8099/websocket";//兼容00版的websocket协议，甭管

    void start();
    void shutdown();
}
