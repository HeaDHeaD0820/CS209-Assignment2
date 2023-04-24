package cn.edu.sustech.cs209.chatting.common;


import com.google.gson.Gson;

public class Message {
    // 用户名重复
    public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
    // 用户名为Server
    public static final String USERNAME_EQUALS_TO_SERVER = "USERNAME_EQUALS_TO_SERVER";
    // 目标用户成功加入聊天室
    public static final String JOIN_SUCCESS = "JOIN_SUCCESS";
    // 告知其他用户有新用户加入聊天室
    public static final String NEW_CLIENT_JOIN_SUCCESS = "NEW_CLIENT_JOIN_SUCCESS";



//    public static final String COMMAND_LOGIN = "LOGIN";
//    public static final String COMMAND_MESSAGE = "MESSAGE";
//    public static final String COMMAND_LOGOUT = "LOGOUT";
//    public static final String COMMAND_CLIENT_LIST = "CLIENT_LIST";

    private final Long timestamp;
    private final String sentBy;
    private final String sendTo;
    private final String type;
    private final String data;

    public Message(Long timestamp, String sentBy, String sendTo, String type, String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.type = type;
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Message fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Message.class);
    }

}