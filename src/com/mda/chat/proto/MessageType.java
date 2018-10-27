package com.mda.chat.proto;

public class MessageType
{
    public  static final int PING=10015;            //ping消息
    public  static final int PONG=10016;            //pong消息

    public  static final int SYST=988; //系统消息
    public  static final int SYS_USER_COUNT=20001;  // 在线用户数
    public  static final int SYS_AUTH_STATE=20002;  // 认证结果
    public  static final int SYS_OTHER_INFO=20003;  // 系统消息

    public  static final int EROR=1244;             //错误消息

    public  static final int AUTH=10000;            //认证消息
    public  static final int MESS=10086;           //普通消息


}
