package cn.edu.sustech.cs209.chatting.client;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class ChattingClient extends Application {
  public static void main(String[] args) {
    launch();
  }

  @Override
  public void start(Stage stage) throws IOException {
    System.setProperty("file.encoding", "UTF-8");
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
    stage.setScene(new Scene(fxmlLoader.load()));
    stage.setTitle("Chatting - Client Terminal");
    stage.show();
    Controller controller = fxmlLoader.getController();
    stage.setOnCloseRequest(event -> controller.shutdown());
  }
}
