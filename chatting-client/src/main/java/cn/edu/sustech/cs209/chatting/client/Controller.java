package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Controller implements Initializable {

  @FXML
  ListView<Message> chatContentList;
  // 记录已经存在的聊天列表
  public ListView<ChatRoom> chatList;

  public TextArea inputArea;
  //
  public Label currentChatTitle;
  // 当前聊天窗对方名称 在请求建立对话中会发给服务器
  private String chatTitle;

  // 对话中显示自己的名字 左下角
  public Label currentUsername;
  // 当前在线人数 右下角
  public Label currentOnlineCnt;

  // 输入输出流
  private BufferedReader in;
  private PrintWriter out;

  // 当前用户用户名
  String username;
  // 当前是否为群聊
  private boolean isGroup;

  // 当前在线用户
  private List<String> curClients = new ArrayList<>();

  // 更新当前在线用户
  public List<String> getCurClients() {
      return curClients;
  }

  // 返回当前在线用户
  public void setCurClients(List<String> newCurClients) {
      curClients = newCurClients;
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    // 初始弹出输入框 定义用户名
    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");

    Optional<String> input = dialog.showAndWait();
    // 得到合法用户名后 以该用户名为身份进入应用
    if (input.isPresent() && !input.get().isEmpty()) {
        username = input.get();
        this.currentUsername.setText(String.format("Current User: %s", this.username));
//        this.inputArea.setWrapText(true);
        this.chatTitle = "";
        try {
            // 创建socket
            Socket clientSocket = new Socket("localhost", 9328);
            // 初始化输入输出流
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            // 通知服务器 请求上线 同时包含了自己的用户名
            Message goOnlineMsg = buildMessage("Server", Message.CLIENT_GO_ONLINE, username);
            sendOutMessage(goOnlineMsg);
            // 监听等待回复
            boolean joinAdmitted = false;
            String received;
            while ((received = in.readLine()) != null) {
                Message received_message = Message.fromJson(received);
                String messageType = received_message.getType();
                String messageData = received_message.getData();
                // server允许用户上线
                if(Objects.equals(messageType, Message.JOIN_SUCCESS)){
                    joinAdmitted = true;
                    break;
                }
                // server拒绝用户上线
                else if(Objects.equals(messageType, Message.JOIN_FAILURE)){
                    if(Objects.equals(messageData, Message.DUPLICATE_USERNAME)){
                        popOutAlert("Join Failure", "Duplicate username");
                        System.out.println("Duplicate username " + input + ", exiting");
                    }else if(Objects.equals(messageData, Message.USERNAME_EQUALS_TO_SERVER)){
                        popOutAlert("Join Failure", "Username cannot be 'Server'");
                        System.out.println("Invalid username " + input + ", exiting");
                    }
                    in.close();
                    out.close();
                    clientSocket.close();
                    Platform.exit();
                    break;
                }
            }
            if(joinAdmitted){
                new Thread(new Controller.ClientMessageThread(clientSocket)).start();
                chatContentList.setCellFactory(new MessageCellFactory());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    } else {
        popOutAlert("Join Failure", "Invalid Username.");
        System.out.println("Invalid username " + input + ", exiting");
        Platform.exit();
    }
  }

  // 弹出提示框提示用户
  public static void popOutAlert(String title, String content) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(content);
      alert.showAndWait();
  }



  // TODO showGroupChatConfigDialog


  // 向服务器说明自己要发起聊天 请求得到私聊聊天记录
  private void launchPrivateChat(String title){
      Message createPrivateChatMsg = buildMessage(chatTitle, Message.LAUNCH_PRIVATE_CHAT, title);
      sendOutMessage(createPrivateChatMsg);
  }


  // TODO
  @FXML
  public void createPrivateChat() {
      AtomicReference<String> user = new AtomicReference<>();
      // 设置窗口
      Stage stage = new Stage();
      stage.setTitle("Select a user for private chat");
      stage.setWidth(400.0);
      stage.setHeight(200.0);
      // 设置下拉栏
      Label userSelLabel = new Label("Select a user:");
      ComboBox<String> selBox = new ComboBox<>();
      selBox.setPrefWidth(200.0);
      Button confirmBtn = new Button("OK");
      synchronized (this) {
          selBox.getItems().addAll(getCurClients());
      }
      // 设置按钮事件
      confirmBtn.setOnAction(e -> {
          user.set(selBox.getSelectionModel().getSelectedItem());
          stage.close();
      });
      // 显示组件
      HBox box = new HBox(12);
      box.setPadding(new Insets(20, 12, 20, 12));
      box.setAlignment(Pos.CENTER);
      box.getChildren().addAll(userSelLabel, selBox, confirmBtn);
      stage.setScene(new Scene(box));
      stage.showAndWait();

      // 获取选中用户的用户名
      String userSelected = user.get();
      if(userSelected != null && !userSelected.equals("")){
          this.chatTitle = userSelected;
          this.currentChatTitle.setText(userSelected);
          this.isGroup = false;
          boolean chatRoomExists = false;
          for (ChatRoom room : chatList.getItems()) {
              System.out.println(userSelected);
              if (room != null && room.getTitle().equals(userSelected)) {
                  chatRoomExists = true;
                  break;
              }
          }
          // 第一次创建时执行if
          if (!chatRoomExists) {
              ChatRoom newChatRoom = new ChatRoom(userSelected);
              newChatRoom.setOnMouseClicked(event -> {
                  System.out.println("Click ChatRoom");
                  Controller.this.isGroup = false;
                  Controller.this.chatTitle = newChatRoom.getTitle();
                  Controller.this.currentChatTitle.setText(userSelected);
                  System.out.println(chatTitle);
                  launchPrivateChat(Controller.this.chatTitle);
              });
              this.chatList.getItems().add(newChatRoom);
          }
          launchPrivateChat(this.chatTitle);
      }
  }



  // 向服务器说明自己要发起聊天 请求得到群聊聊天记录
  private void launchGroupChat(String users){
      Message createGroupChatMsg = buildMessage(chatTitle, Message.LAUNCH_GROUP_CHAT, users);
      sendOutMessage(createGroupChatMsg);
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name.
   * You can select several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat:
   * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
   * UserA, UserB, UserC... (10)
   * If there are <= 3 users: do not display the ellipsis, for example:
   * UserA, UserB (2)
   */


  @FXML
  public void createGroupChat() {
      AtomicReference<List<String>> selectedUsers = new AtomicReference<>(new ArrayList<>());
      // 设置窗口
      Stage stage = new Stage();
      stage.setTitle("Select users for group chat");
      stage.setWidth(400.0);
      stage.setHeight(300.0);
      // 设置多选框
      Label userSelLabel = new Label("Select users:");
      VBox box = new VBox(12);
      box.setPadding(new Insets(20, 12, 20, 12));
      box.setAlignment(Pos.CENTER_LEFT);
      synchronized (this) {
          List<String> curClients = getCurClients();
          for (String client : curClients) {
              CheckBox checkBox = new CheckBox(client);
              checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                  if (newValue) {
                      selectedUsers.get().add(client);
                  } else {
                      selectedUsers.get().remove(client);
                  }
              });
              box.getChildren().add(checkBox);
          }
      }
      // 设置按钮事件
      Button confirmBtn = new Button("OK");
      confirmBtn.setOnAction(e -> {
          stage.close();
      });
      // 显示组件
      ScrollPane scrollPane = new ScrollPane(box);
      scrollPane.setFitToWidth(true);
      scrollPane.setPrefHeight(200.0);
      scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
      scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
      VBox layout = new VBox(12);
      layout.getChildren().addAll(userSelLabel, scrollPane, confirmBtn);
      layout.setAlignment(Pos.CENTER);
      layout.setPadding(new Insets(20, 12, 20, 12));
      stage.setScene(new Scene(layout));
      stage.showAndWait();

      // 从selectedUsers中提取用户名
      List<String> usernames = selectedUsers.get();
      if (usernames.isEmpty()) {
          return;
      }
      String title_comma;
      usernames.add(username);
      String formattedTitle;
      if (usernames.size() > 3) {
          String ellipsis = String.format("... (%d)", usernames.size());
          title_comma = usernames.stream().sorted().limit(3).collect(Collectors.joining(", "));
          formattedTitle = String.format("%s%s", title_comma, ellipsis);

      } else {
          title_comma = usernames.stream().sorted().collect(Collectors.joining(", "));
          formattedTitle = title_comma;
      }
      currentChatTitle.setText(formattedTitle);

      // 生成chatTitle
      String chatTitleJoined = usernames.stream().sorted().collect(Collectors.joining("$"));
      chatTitle = chatTitleJoined;

      // 遍历chatList，如果ChatRoom不存在则创建
      boolean chatRoomExists = false;
      for (ChatRoom room : chatList.getItems()) {
          if (room != null && room.getTitle().equals(chatTitleJoined)) {
              chatRoomExists = true;
              break;
          }
      }
      if (!chatRoomExists) {
          ChatRoom newChatRoom = new ChatRoom(formattedTitle);
          newChatRoom.setOnMouseClicked(event -> {
              System.out.println("Click Group ChatRoom");
              isGroup = true;
              chatTitle = chatTitleJoined;
              currentChatTitle.setText(formattedTitle);
              launchGroupChat(chatTitle);
          });
          chatList.getItems().add(newChatRoom);
      }
      launchGroupChat(chatTitle);
  }


  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed.
   * After sending the message, you should clear the text input field.
   */


  // TODO
  @FXML
  public void doSendMessage() {
    String textContent = inputArea.getText();
    if(textContent.length()>0){
      if(!textContent.replaceAll("(?!\\r)\\n", "").equals("")){
        inputArea.setText("");
        String messageType = "";
        if(isGroup){
          messageType = Message.SEND_GROUP_MESSAGE;
        }else{
          messageType = Message.SEND_PRIVATE_MESSAGE;
        }
        // 把对话框里的对话消息发给服务器
        Message contentMsg = buildMessage(this.chatTitle, messageType, textContent);
        sendOutMessage(contentMsg);
      }
    }
    else{
        popOutAlert("Warning", "Blank messages are not allowed.");
    }
  }



  // shutdown让当前client向服务器发送消息告知自己下线
  public void shutdown() {
    Message goOfflineMsg = buildMessage("Server", Message.CLIENT_GO_OFFLINE, Message.CLIENT_GO_OFFLINE);
    sendOutMessage(goOfflineMsg);
  }

  // 发送socket层面的消息
  public void sendOutMessage(Message message){
    out.println(message.toJson());
  }

  private Message buildMessage(String target, String type, String data) {
    return new Message(System.currentTimeMillis(), this.username, target, type, data);
  }

  // 待修改
  private class ClientMessageThread implements Runnable {
    private final Socket clientSocket;

  public ClientMessageThread(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

    public void run() {
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
        String received;
        while ((received = in.readLine()) != null) {
          Message received_message = Message.fromJson(received);
          System.out.println(received_message.toString());
          String messageType = received_message.getType();
          String messageData = received_message.getData();
          // 用户收到同意下线 就直接下线
          if(messageType.equals(Message.SERVER_AGREE_GO_OFFLINE)){
            break;
          }
          else if(messageType.equals(Message.UPDATE_LIST)){
            // 用户列表算出人数显示在界面
            System.out.println("Refresh List Here");
            List<String> usernames = new ArrayList<>(Arrays.asList(messageData.substring(1, messageData.length() - 1).split(", ")));
            int cnt_user = usernames.size();
            Platform.runLater(() -> {
                Controller.this.currentOnlineCnt.setText("Online: " + cnt_user);
            });
            usernames.remove(username);
            // 用户列表去掉自己的名字
            synchronized (Controller.this) {
                setCurClients(usernames);
            }
          }
          // 发起私聊的服务器响应 主要返回聊天记录
          else if(Objects.equals(messageType, Message.RE_PRIVATE_CHAT)){
            // 接收到数据处理回Message List
            ArrayList<Message> msgList = new ArrayList<>();
            if(!messageData.isEmpty()){
              for (String msgStr : messageData.split("@_@")) {
                Message message = Message.fromJson(msgStr);
                msgList.add(message);
              }
            }
            if (!isGroup) {
              String curKey;
              if (Controller.this.username.compareTo(Controller.this.chatTitle) < 0) {
                curKey = Controller.this.username + "_" + Controller.this.chatTitle;
              } else {
                curKey = Controller.this.chatTitle + "_" + Controller.this.username;
              }
              String returnKey;
              if (msgList.size() > 0){
                if (msgList.get(0).getSentBy().compareTo(msgList.get(0).getSendTo()) < 0) {
                  returnKey = msgList.get(0).getSentBy() + "_" + msgList.get(0).getSendTo();
                } else {
                  returnKey = msgList.get(0).getSendTo() + "_" + msgList.get(0).getSentBy();
                }
                if(returnKey.equals(curKey)){
                  Platform.runLater(() -> {
                    Controller.this.chatContentList.getItems().clear();
                    Controller.this.chatContentList.getItems().addAll(msgList);
                  });
                }
              }
              else {
                Controller.this.chatContentList.getItems().clear();
              }
            }
          }
          // 发起群聊的服务器响应 主要返回聊天记录
          else if(Objects.equals(messageType, Message.RE_GROUP_CHAT)){
            // 接收到数据处理回Message List
            ArrayList<Message> msgList = new ArrayList<>();
            if(!messageData.isEmpty()){
              for (String msgStr : messageData.split("@_@")) {
                Message message = Message.fromJson(msgStr);
                msgList.add(message);
              }
            }
            if (isGroup) {
              if (msgList.size() > 0){
                if (chatTitle.equals(received_message.getSendTo())) {
                  Platform.runLater(() -> {
                    Controller.this.chatContentList.getItems().clear();
                    Controller.this.chatContentList.getItems().addAll(msgList);
                  });
                }
              }
              else {
                Controller.this.chatContentList.getItems().clear();
              }
            }

          }
        }
        in.close();
        System.out.println("ClientMessageThread closed.");
        Platform.exit();
      } catch (SocketException e) {
        System.out.println("Disconnected from the server.");
        popOutAlert("Disconnected", "Disconnected from the server.");
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          clientSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }


  /**
   * You may change the cell factory if you changed the design of {@code Message} model.
   * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
              setText(null);
              setGraphic(null);
              return;
          }
          //
          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());

          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentBy())) {
              wrapper.setAlignment(Pos.TOP_RIGHT);
              wrapper.getChildren().addAll(msgLabel, nameLabel);
              msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
              wrapper.setAlignment(Pos.TOP_LEFT);
              wrapper.getChildren().addAll(nameLabel, msgLabel);
              msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }
          //

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }
}
