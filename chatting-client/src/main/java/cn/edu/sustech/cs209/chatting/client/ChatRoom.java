package cn.edu.sustech.cs209.chatting.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ChatRoom extends HBox {

  private final Label title;

  public ChatRoom(String title) {
    super(10); // spacing between children
    this.title = new Label(title);
    this.title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

    Label time = new Label(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    time.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
    time.setTextFill(Color.GRAY);

    VBox nameTime = new VBox(30, this.title, time);
    nameTime.setAlignment(Pos.CENTER_LEFT);
    this.getChildren().addAll(nameTime);
  }

  public String getTitle() {
    return this.title.getText();
  }
}
