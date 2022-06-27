package ru.gb.networkchat_v2.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.gb.networkchat_v2.Command;

import java.io.IOException;
//Класс описывает пользовательское окно(stage) и сцену(scene), которая содержит элементы пользовательского интерфейса, которые описаны в client-view.fxml файле
public class ChatClientApp extends Application {
    //Описываем пользовательское окно и сцену
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatClientApp.class.getResource("client-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Chat client");
        stage.setScene(scene);
        stage.show();

        ChatController controller = fxmlLoader.getController();
        stage.setOnCloseRequest(event -> controller.getClient().sendMessage(Command.END));
    }
    //Запускаем приложение
    public static void main(String[] args) {
        launch();
    }
}