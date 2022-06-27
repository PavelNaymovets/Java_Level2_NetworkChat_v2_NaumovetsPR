package ru.gb.networkchat_v2.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ru.gb.networkchat_v2.Command;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
//Класс описывает поведение элементов пользовательского интерфейса
public class ChatController {
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextField loginField; //Поле ввода логина
    @FXML
    private PasswordField passField; //Поле ввода пароля
    @FXML
    private HBox authBox; //Определяет видимость стартового блока утентификации (логин, пароль, войти)
    @FXML
    private HBox messageBox; //Определяет видимость блока с сообщениями
    @FXML
    private TextArea messageArea; //Окно истории чата
    @FXML
    private TextField messageField; //Поле для ввода сообщений
    private ChatClient client; //экземпляр класса, описывающего логику работы программы на стороне клиента. Нужен, для взаимодействия визуальной и серверной частей приложения

    private String selectedNick;
    public ChatController() {
        this.client = new ChatClient(this); //Создаем экземпляр класса клиента, передаем в него экземпляр класса контроллера, для взаимодействия визуальной и серверной частей приложения
        while (true) {
            try {
                client.openConnection(); //Открываем соединение с сервером (создаем канал связи - сокет)
                break;
            } catch (IOException e) {
                showNotification(); // Если соединение с сервером не удалось установить, показываем уведомление о неудачном подключении
            }
        }

    }

    //Уведомление. Визуальная и функциональная части.
    private void showNotification() {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Не могу подключиться к серверу. Проверьте, что сервер запущен и доступен",
                new ButtonType("Попробовать снова", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Выйти", ButtonBar.ButtonData.CANCEL_CLOSE)
        );
        alert.setTitle("Ошибка подключения");
        //Optional - контейнер. В нем либо, что-то лежит, либо нет.
        Optional<ButtonType> userAnswer = alert.showAndWait();
        //Для получения значения из optional используем метод .map(). В answer лежит кнопка, на которую нажал пользователь.
        Boolean isExit = userAnswer.map(select -> select.getButtonData().isCancelButton()).orElse(false);
        if(isExit){
            System.exit(0);
        }
    }

    //Работа кнопки по отправлению сообщения в чат
    public void clickSendButton() {
        String sendMessage = messageField.getText(); //Получаю текст из строки ввода сообщения
        if (sendMessage.isBlank()) { //Если строка содержит только пробелы, ничего не происходит
            return;
        }
        if(selectedNick != null){//Отправляю выбранному из списка клиенту сообщение
            client.sendMessage(Command.PRIVATE_MESSAGE, selectedNick, sendMessage);
            selectedNick = null;
        } else {
            client.sendMessage(Command.MESSAGE, sendMessage); //Отправляю скопированное сообщение на сервер
        }
        messageField.clear(); //Чищу поле ввода сообщений
        messageField.requestFocus();
    }
    //Добавление сообщений в окно истории чата
    public void addMessage(String message) {
        messageArea.appendText(message + "\n"); //Добавляю сообщение в окно истории чата
    }

    //Регулирую видимость блока аутентификации и блока чата
    public void setAuth(boolean success){
        authBox.setVisible(!success); //Если пользователь определен, отключаю видимость блока аутентификации
        messageBox.setVisible(success); //Включаю видимсоть блока чата
    }
    //Вход в чат
    public void signinButtonClick(ActionEvent actionEvent) {
        client.sendMessage(Command.AUTH, loginField.getText(), passField.getText()); //Отправляю логин и пароль на сервер для аутентификации
        loginField.clear(); //Чищу поле логина
        passField.clear(); //Чищу поле пароля
    }

    public void showError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR, errorMessage, new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        alert.setTitle("Error!");
        alert.showAndWait();
    }

    //Выбираю клиента из списка
    public void SelectClient(MouseEvent mouseEvent) {
        if(mouseEvent.getClickCount() == 2){
            String selectedNick = clientList.getSelectionModel().getSelectedItem();
            if(selectedNick != null && !selectedNick.isEmpty()){
                this.selectedNick = selectedNick;
            }
        }
    }

    public void updateClientList(String[] clients) {
        clientList.getItems().clear();
        clientList.getItems().addAll(clients);
    }

    public void signOutClick(ActionEvent actionEvent) {
        client.sendMessage(Command.END);
    }

    public ChatClient getClient() {
        return client;
    }
}