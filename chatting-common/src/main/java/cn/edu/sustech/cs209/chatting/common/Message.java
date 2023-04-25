package cn.edu.sustech.cs209.chatting.common;


import com.google.gson.Gson;

public class Message {

    // type:包含以下两种非法用户名
    public static final String JOIN_FAILURE = "JOIN_FAILURE";
    // 用户名重复
    public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
    // 用户名为Server
    public static final String USERNAME_EQUALS_TO_SERVER = "USERNAME_EQUALS_TO_SERVER";
    // 告知目标用户成功加入聊天室
    public static final String JOIN_SUCCESS = "JOIN_SUCCESS";
    // 告知其他用户更新列表
    public static final String UPDATE_LIST = "UPDATE_LIST";
    // 用户向服务器请求下线
    public static final String CLIENT_GO_OFFLINE = "CLIENT_GO_OFFLINE";
    // 用户向服务器请求上线
    public static final String CLIENT_GO_ONLINE = "CLIENT_GO_ONLINE";
    // 服务器同意下线
    public static final String SERVER_AGREE_GO_OFFLINE = "SERVER_AGREE_GO_OFFLINE";
    // 服务器同意上线
    public static final String SERVER_AGREE_GO_ONLINE = "SERVER_AGREE_GO_ONLINE";
    // 服务器广播通知所有人 有人下线了
    public static final String NOTICE_CLIENT_OFFLINE = "NOTICE_CLIENT_OFFLINE";

    // 用户创建私聊
    public static final String LAUNCH_PRIVATE_CHAT = "LAUNCH_PRIVATE_CHAT";
    // 用户创建群聊
    public static final String LAUNCH_GROUP_CHAT = "LAUNCH_GROUP_CHAT";
    // 用户发私聊消息
    public static final String SEND_PRIVATE_MESSAGE = "SEND_PRIVATE_MESSAGE";
    // 用户发群聊消息
    public static final String SEND_GROUP_MESSAGE = "SEND_GROUP_MESSAGE";
    // 返回私聊聊天记录
    public static final String RE_PRIVATE_CHAT = "RE_PRIVATE_CHAT";
    // 返回群聊聊天记录
    public static final String RE_GROUP_CHAT = "RE_GROUP_CHAT";

    // 换行编码
    public static final String NEW_LINE = "NEW_LINE";


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

    @Override
    public String toString() {
        return "Message{" +
                "timestamp=" + timestamp +
                ", sentBy='" + sentBy + '\'' +
                ", sendTo='" + sendTo + '\'' +
                ", type='" + type + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}