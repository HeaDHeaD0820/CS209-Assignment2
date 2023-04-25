package cn.edu.sustech.cs209.chatting.client;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatRoom extends HBox {

    private Label title;
    private Label time;
    public ChatRoom(String title){
        super(10); // spacing between children
        this.title = new Label(title);
        this.title.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        this.time = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        this.time.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        this.time.setTextFill(Color.GRAY);

        VBox nameTime = new VBox(5, this.title, this.time);
        nameTime.setAlignment(Pos.CENTER_LEFT);
    }

    public String getTitle() {
        return this.title.getText();
    }
}
