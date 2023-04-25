package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class ChattingServer {

  private static final int PORT = 9328;
  private static final ArrayList<ClientThread> clients = new ArrayList<>();

  private static final HashMap<String, ArrayList<Message>> privateMessageHistory = new HashMap<>();
  private static final HashMap<String, ArrayList<Message>> groupMessageHistory = new HashMap<>();
  private static final HashMap<String, ArrayList<String>> groupMemebersMap = new HashMap<>();

  public static void main(String[] args) {
    try {
      ServerSocket serverSocket = new ServerSocket(PORT);
      System.out.println("Starting Server on PORT " + PORT);
      // Server保持运行 持续接收客户端连接请求
      while (true) {
        // 阻塞等待
        Socket clientSocket = serverSocket.accept();
        // Print IP in String
        System.out.println("New Client " + clientSocket.getInetAddress().getHostAddress()
                + " Connected");
        // 为客户端clientSocket创建ClientThread
        ChattingServer.ClientThread clientThread = new ClientThread(clientSocket);
        String clientUsername = clientThread.getUsername();
        // 用户名为Server
        if (checkIfUsernameServer(clientUsername)) {
          Message usernameEqualsToServerMsg = buildMessage(clientUsername, Message.JOIN_FAILURE,
                  Message.USERNAME_EQUALS_TO_SERVER);
          clientThread.sendMessage(usernameEqualsToServerMsg);
          continue;
        }
        // 用户名重复
        if (!checkIfUsernameUnique(clientUsername)) {
          Message usernameEqualsToServerMsg = buildMessage(clientUsername, Message.JOIN_FAILURE,
                  Message.DUPLICATE_USERNAME);
          clientThread.sendMessage(usernameEqualsToServerMsg);
          continue;
        }
        // 该用户成功上线 加入列表+告知用户
        clients.add(clientThread);
        clientThread.start();
        Message joinSuccessMsg = buildMessage(clientUsername,
                Message.JOIN_SUCCESS, Message.JOIN_SUCCESS);
        clientThread.sendMessage(joinSuccessMsg);
        // 告知所有用户当前用户列表 包括新用户
        ArrayList<String> usernames = new ArrayList<>();
        for (ClientThread client : clients) {
          usernames.add(client.getUsername());
        }
        broadcastMessage(Message.UPDATE_LIST, usernames.toString());
        System.out.println(usernames);
        System.out.println(joinSuccessMsg);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  // 客户端向所有在线Client广播Message
  private static void broadcastMessage(String type, String data) {
    for (ClientThread c : clients) {
      Message message = buildMessage(c.getUsername(), type, data);
      c.sendMessage(message);
    }
  }


  // 所有服务器消息的前两个参数相同
  private static Message buildMessage(String targetClient, String type, String data) {
    return new Message(System.currentTimeMillis(), "Server", targetClient, type, data);
  }

  // 检查用户Username是否重复
  private static boolean checkIfUsernameUnique(String username) {
    for (ClientThread c : clients) {
      if (c.getUsername().equals(username)) {
        return false;
      }
    }
    return true;
  }

  // 检查用户Username是否为Server
  private static boolean checkIfUsernameServer(String username) {
    return username.equalsIgnoreCase("Server");
  }

  // ClientThread用于服务器与各个Client通讯
  public static class ClientThread extends Thread {
    private final String username;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    // 初始化时定义输入in 输出out 和 socket
    public ClientThread(Socket clientSocket) throws IOException {
      this.socket = clientSocket;
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
              StandardCharsets.UTF_8));
      out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),
              StandardCharsets.UTF_8), true);

      this.username = Message.fromJson(in.readLine()).getData();
    }

    // 获取客户端username
    public String getUsername() {
      return username;
    }

    // 向目标客户端发送message
    public void sendMessage(Message message) {
      out.println(message.toJson());
    }

    // 在客户端移除下线用户
    public synchronized void removeClient(ClientThread clientThread) {
      clients.remove(clientThread);
    }

    public void run() {
      System.out.println("Client " + socket.getInetAddress()
              + getUsername() + " connected to Server");
      try {
        String received;
        while ((received = in.readLine()) != null) {
          Message received_message = Message.fromJson(received);
          System.out.println(received_message.toString());
          String messageType = received_message.getType();
          String messageData = received_message.getData();
          // 用户下线 同意其下线
          if (Objects.equals(messageType, Message.CLIENT_GO_OFFLINE)) {
            // 通知他可以下线了
            Message agreeMsg = buildMessage(getUsername(),
                     Message.SERVER_AGREE_GO_OFFLINE, Message.SERVER_AGREE_GO_OFFLINE);
            sendMessage(agreeMsg);
            // 从列表中移除为他准备的线程
            removeClient(this);
            // 告诉所有人他下线了
            broadcastMessage(Message.NOTICE_CLIENT_OFFLINE, getUsername());
            // 让所有人更新用户列表
            ArrayList<String> usernames = new ArrayList<>();
            for (ClientThread client : clients) {
              usernames.add(client.getUsername());
            }
            broadcastMessage(Message.UPDATE_LIST, usernames.toString());
            break;
          } else if (Objects.equals(messageType, Message.LAUNCH_PRIVATE_CHAT)) {       // 用户发起私聊
            String sender = received_message.getSentBy();
            String receiver = received_message.getSendTo();
            String key;
            if (sender.compareTo(receiver) < 0) {
              key = sender + "_" + receiver;
            } else {
              key = receiver + "_" + sender;
            }
            if (!privateMessageHistory.containsKey(key)) {
              privateMessageHistory.put(key, new ArrayList<>());
            }
            sendPriMsgHistory(received_message, key);
          } else if (Objects.equals(messageType, Message.LAUNCH_GROUP_CHAT)) { // 用户发起群聊
            String receivers = received_message.getSendTo();
            if (!groupMessageHistory.containsKey(receivers)) {
              groupMessageHistory.put(receivers, new ArrayList<>());
              ArrayList<String> usernamesList = new ArrayList<>(
                      Arrays.asList(receivers.split("\\$")));
              groupMemebersMap.put(receivers, usernamesList);
            }
            sendGrpMsgHistory(receivers);
          } else if (Objects.equals(messageType, Message.SEND_PRIVATE_MESSAGE)) { //用户发私聊消息
            String sender = received_message.getSentBy();
            String receiver = received_message.getSendTo();
            String key;
            if (sender.compareTo(receiver) < 0) {
              key = sender + "_" + receiver;
            } else {
              key = receiver + "_" + sender;
            }
            if (!privateMessageHistory.containsKey(key)) {
              privateMessageHistory.put(key, new ArrayList<>());
            }
            // 和发起时的唯一区别是需要添加当前消息received_message
            privateMessageHistory.get(key).add(received_message);
            sendPriMsgHistory(received_message, key);
          } else if (Objects.equals(messageType, Message.SEND_GROUP_MESSAGE)) { // 用户发群聊消息
            String receivers = received_message.getSendTo();
            groupMessageHistory.get(receivers).add(received_message);
            sendGrpMsgHistory(receivers);
          }
        }
      } catch (IOException ex) {
        System.out.println("Catch exception and exit");
      } finally {
        try {
          in.close();
          out.close();
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    private void sendPriMsgHistory(Message message, String key) {
      List<Message> msgList = privateMessageHistory.get(key);
      List<String> strList = msgList.stream().map(Message::toJson).collect(Collectors.toList());
      String joinedString = String.join("@_@", strList);
      // 聊天记录处理拼接后原路返回给请求者 并且找到接收者 也给他发一份
      String sender = message.getSentBy();
      String receiver = message.getSendTo();
      Message resMsgToSender = buildMessage(sender, Message.RE_PRIVATE_CHAT, joinedString);
      this.sendMessage(resMsgToSender);
      System.out.println(receiver);
      System.out.println(joinedString);
      for (ClientThread cthrd : clients) {
        if (cthrd.getUsername().equals(receiver)) {
          Message resMsgToReceiver = buildMessage(receiver, Message.RE_PRIVATE_CHAT, joinedString);
          cthrd.sendMessage(resMsgToReceiver);
          break;
        }
      }
    }

    private void sendGrpMsgHistory(String key) {
      // 消息记录列表
      List<Message> msgList = groupMessageHistory.get(key);
      List<String> strList = msgList.stream().map(Message::toJson).collect(Collectors.toList());
      String joinedString = String.join("@_@", strList);
      // 成员username列表
      List<String> memberList = groupMemebersMap.get(key);
      for (String member : memberList) {
        for (ClientThread cthrd : clients) {
          if (cthrd.getUsername().equals(member)) {
            Message resMsgToMembers = buildMessage(key, Message.RE_GROUP_CHAT, joinedString);
            cthrd.sendMessage(resMsgToMembers);
            break;
          }
        }
      }
    }
  }
}
