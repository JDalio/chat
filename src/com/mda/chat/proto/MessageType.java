package com.mda.chat.proto;

public enum MessageType
{
    PING(10015),            //ping消息
    PONG(10016),            //pong消息

    SYST(988), //系统消息
    SYS_USER_COUNT(20001),  // 在线用户数
    SYS_AUTH_STATE(20002),  // 认证结果
    SYS_OTHER_INFO(20003),  // 系统消息

    EROR(1244),             //错误消息

    AUTH(10000),            //认证消息
    MESS(10086);            //普通消息


    private int value;

    MessageType(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    public static MessageType valueOf(int num)
    {
        switch (num)
        {
            case 10015:
                return PING;
            case 10016:
                return PONG;
            case 988:
                return SYST;
            case 20001:
                return SYS_USER_COUNT;
            case 20002:
                return SYS_AUTH_STATE;
            case 20003:
                return SYS_OTHER_INFO;
            case 1244:
                return EROR;
            case 10000:
                return AUTH;
            case 10086:
                return MESS;
        }
    }
}
