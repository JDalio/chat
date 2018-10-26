package com.mda.chat.proto;

import com.alibaba.fastjson.JSONObject;
import com.mda.chat.utils.DateTimeUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息对象
 */
public class Message
{
    private int version = 1;
    private int type;
    private Map<String, Object> extend = new HashMap<>();

    private String body;

    public Message(int type, String body)
    {
        this.type = type;
        this.body = body;
    }

    public static String buildProto(int type, String body)
    {
        Message msg = new Message(type, body);
        return JSONObject.toJSONString(msg);
    }

    public static String buildPingProto()
    {
        return buildProto(MessageType.PING.getValue(), null);
    }

    public static String buildPongProto()
    {
        return buildProto(MessageType.PONG.getValue(), null);
    }

    public static String buildSystProto(int code, Object mess)
    {
        Message msg = new Message(MessageType.SYST.getValue(), null);
        msg.extend.put("code", code);
        msg.extend.put("mess", mess);
        return JSONObject.toJSONString(msg);
    }

    public static String buildAuthProto(boolean isSuccess)
    {
        Message msg = new Message(MessageType.AUTH.getValue(), null);
        msg.extend.put("isSuccess", isSuccess);
        return JSONObject.toJSONString(msg);
    }

    public static String buildErorProto(int code, String mess)
    {
        Message msg = new Message(MessageType.EROR.getValue(), null);
        msg.extend.put("code", code);
        msg.extend.put("mess", mess);
        return JSONObject.toJSONString(msg);
    }

    public static String buildMessProto(int uid, String nick, String mess)
    {
        Message msg = new Message(MessageType.MESS.getValue(), mess);
        msg.extend.put("uid", uid);
        msg.extend.put("nick", nick);
        msg.extend.put("time", DateTimeUtil.getCurrentTime());
        return JSONObject.toJSONString(msg);
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public Map<String, Object> getExtend()
    {
        return extend;
    }

    public void setExtend(Map<String, Object> extend)
    {
        this.extend = extend;
    }
}
