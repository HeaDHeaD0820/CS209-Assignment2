package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ChattingServer {

    private static final int PORT = 9328;
    private static final ArrayList<ClientThread> clients = new ArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Starting Server on PORT " + PORT);
            // Server保持运行 持续接收客户端连接请求
            while (true) {
                // 阻塞等待
                Socket clientSocket = serverSocket.accept();
                // Print IP in String
                System.out.println("New Client " + clientSocket.getInetAddress().getHostAddress() + " Connected");
                // 为客户端clientSocket创建ClientThread
                ClientThread clientThread = new ClientThread(clientSocket);
                String clientUsername = clientThread.getUsername();
                // 用户名为Server 重新循环
                if(checkIfUsernameServer(clientUsername)){
                    Message usernameEqualsToServerMsg = buildMessage(clientUsername, Message.DUPLICATE_USERNAME, Message.DUPLICATE_USERNAME);
                    clientThread.sendMessage(usernameEqualsToServerMsg);
                    continue;
                }
                // 用户名重复 重新循环
                if(!checkIfUsernameUnique(clientUsername)){
                    Message usernameEqualsToServerMsg = buildMessage(clientUsername, Message.USERNAME_EQUALS_TO_SERVER, Message.USERNAME_EQUALS_TO_SERVER);
                    clientThread.sendMessage(usernameEqualsToServerMsg);
                    continue;
                }
                // 该用户成功上线 加入列表+告知用户
                clients.add(clientThread);
                clientThread.start();
                Message joinSuccessMsg = buildMessage(clientUsername, Message.JOIN_SUCCESS, Message.JOIN_SUCCESS);
                clientThread.sendMessage(joinSuccessMsg);
                // 告知所有用户当前用户列表 包括新用户
                // TODO JSON?clients列表怎么传好一些
                Message newClientJoinMsg = buildMessage(clientUsername, Message.NEW_CLIENT_JOIN_SUCCESS, clients.toString());
                broadcast(newClientJoinMsg);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    // 客户端向所有在线Client广播Message
    private static void broadcast(Message message){
        for(ClientThread c: clients){
            c.sendMessage(message);
        }
    }


    // 所有服务器消息的前两个参数相同
    private static Message buildMessage(String targetClient, String type, String data) {
        return new Message(System.currentTimeMillis(), "Server", targetClient, type, data);
    }

    // 检查用户Username是否重复
    private static boolean checkIfUsernameUnique(String username){
        for(ClientThread c: clients){
            if(c.getUsername().equals(username)){
                return false;
            }
        }
        return true;
    }

    // 检查用户Username是否为Server
    private static boolean checkIfUsernameServer(String username){
        return username.equals("Server");
    }

//    public void removeClient(ClientThread client) {
//        clients.remove(client);
//    }

    // ClientThread用于服务器与各个Client通讯
    public static class ClientThread extends Thread{
        private String username;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        // 初始化时定义输入in 输出out 和 socket
        public ClientThread(Socket clientSocket) throws IOException {
            this.socket = clientSocket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        // 获取客户端username
        public String getUsername(){
            return username;
        }

        // 向目标客户端发送message
        public void sendMessage(Message message){
            out.println(message.toJson());
        }

        public void removeClient(ClientThread clientThread){
            clients.remove(clientThread);
        }

        public void run() {
            System.out.println("Starting Client Thread " + getUsername());
            while(true){

            }


        }
    }

}
