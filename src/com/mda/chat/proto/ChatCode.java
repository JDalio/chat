package com.mda.chat.proto;

public class ChatCode
{
    /**
     * both client and sever use the same ping/pong code
     */
    public static final int PING = 10015;
    public static final int PONG = 10016;


    public static final int AUTH = 10000;
    public static final int MESS = 10086;

    /**
     * 系统消息类型
     */
    public static final int SYS_USER_COUNT = 20001; // 在线用户数
    public static final int SYS_AUTH_STATE = 20002; // 认证结果
    public static final int SYS_OTHER_INFO = 20003; // 系统消息

}
