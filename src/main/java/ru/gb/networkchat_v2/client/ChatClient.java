package ru.gb.networkchat_v2.client;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gb.networkchat_v2.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static ru.gb.networkchat_v2.Command.*;

//Класс описывает логику работы приложения на стороне клиента
public class ChatClient {
    private Socket socket; //Точка соединения с сервером
    private DataInputStream in; //Поток получения информации
    private DataOutputStream out; //Поток передачи информации
    private ChatController controller; //Экземпляр класса, который описывает поведение элементов пользовательского интерфейса. Нужен для взаимодействия с интерфейсом пользователя
    private String nick;
    private String login;
    private Path msgHistoryFile; //Путь к файлу с историей сообщений
    private Logger log; //Логирование работы программы

    public ChatClient(ChatController controller) {
        this.controller = controller;
    }

    public void openConnection() throws IOException {
        socket = new Socket("localhost", 8189); //Создаем точку подключения к серверу. В данном случае мой компьютер
        in = new DataInputStream(socket.getInputStream()); //Открываем поток получения информации
        out = new DataOutputStream(socket.getOutputStream()); //Открываем поток передачи инфомарции
        log = LoggerFactory.getLogger(ChatClient.class); //Адрес класса в котором будет осуществляться логирование
        new Thread(() -> {
            try {
                if (waitAuth()) {//Ожидаю аутентификации пользователя
                    readMessages();//Читаю сообщения от других бользователей
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection();//Закрываю соединение если "/end"
            }
        }).start();

    }

    private boolean waitAuth() throws IOException {
        Thread thread = new Thread(() -> {//Отсчет времени авторизации пользователя
            for (int i = 120; i >= 0 ; i--) {
                try {
                    Thread.sleep(1000);
                    controller.getTimeOutAuth().setText("Время для входа в чат: " + String.valueOf(i));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        while (true) {
            String message = in.readUTF();//Читаю ответ от сервера
            Command command = getCommand(message);
            String[] params = command.parse(message);
            if (command == AUTHOK) {//Если пользователь аутентифицирован, то вернется сообщение в формате authok nick1
                nick = params[0];
                login = params[1];
                createHistoryMessageFile(); // Создаю файл для хранения истории сообщений, если его ещё нет.
                readMessageHistoryFromFile();
                controller.setVisibleTimeOut(false);
                controller.setAuth(true);//Делаю блок чата видимым
                log.info("Успешная авторизация под ником " + nick);
                controller.addMessage("Успешная авторизация под ником " + nick);//Передаю сообщение в окно истории чата только для себя
                return true;
            }
            if (command == ERROR) {
                log.error(params[0]);
                Platform.runLater(() -> controller.showError(params[0]));
                continue;
            }
            if (command == FINISH) { //Таблетка на случай, если время подключения к серверу истечет
                log.warn("Пользователь не вошел в чат. Время для входа закончилось.");
                Platform.runLater(() -> controller.showError("Истекло время на вход в чат. Пожалуйста, перезапустите приложение "));
                try {
                    Thread.sleep(5000); //Нужно, чтобы пользователь увидел сообщение об ошибке
                    sendMessage(END);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return false;
            }
        }
    }

    //Закрываю ресурсы соединения с сервером
    private void closeConnection() {
        log.warn("Соединение с сервером закрыто.");
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    //Читаю сообщения от сервера
    private void readMessages() throws IOException {
        while (true) {
            String receivedMessage = in.readUTF();
            Command command = getCommand(receivedMessage);
            if (END == command) { //Если пользователь захотел отключиться от чата, направляет "/end"
                log.warn("Пользователь: " + nick + " покинул чат.");
                controller.setAuth(false); //Скрываем блок чата. Показываем блок авторизации
                break;
            }
            String[] params = command.parse(receivedMessage);
            if (ERROR == command) {
                String messageError = params[0];
                log.error(messageError);
                Platform.runLater(() -> controller.showError(messageError));
                continue;
            }
            if (MESSAGE == command) {
                log.info(params[0]);
                Platform.runLater(() -> controller.addMessage(params[0])); //Добавляю сообщение от сервера в окно истории чата
                writeMessageHistoryInFile(params[0] + "\n");
            }
            if (CLIENTS == command) {
                log.warn("Обновилась панель с пользователями.");
                Platform.runLater(() -> controller.updateClientList(params));
            }
            if (NICK_BUSY == command){
                String messageError = params[0];
                log.info(messageError);
                Platform.runLater(() -> controller.showError(messageError));
            }
        }
    }

    //Отправить сообщение на сервер
    private void sendMessage(String sendMessage) {
        try {
            out.writeUTF(sendMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Command command, String... params) {
        sendMessage(command.collectMessage(params));
    }

    //Создает файл с историей записей чата
    private void createHistoryMessageFile() {
        msgHistoryFile = Path.of("src", "main", "resources", "ru", "gb", "networkchat_v2", "client", "history_" + login + ".txt");
        if (!Files.exists(msgHistoryFile)) {
            try {
                Files.createFile(msgHistoryFile);
            } catch (IOException e) {
                log.error("Путь для создания файла с историей сообщений не существует " + e.getMessage());
            }
        }
    }

    //Записывает историю сообщений в файл
    private void writeMessageHistoryInFile(String msg) {
        try {
            Files.writeString(msgHistoryFile, msg, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Файла для записи истории сообщений не существует " + e.getMessage());
        }
    }

    //Вычитываю историю сообщений из файла истории сообщений
    private void readMessageHistoryFromFile() {
        if (Files.exists(msgHistoryFile)) {
            try {
                List<String> msgStrings = Files.readAllLines(msgHistoryFile);
                if (msgStrings.size() > 0) {
                    int maxStrings = Math.min(msgStrings.size(), 100);
                    controller.addMessage("*** предыдущие сообщения ***");
                    for (int i = msgStrings.size() - maxStrings; i < msgStrings.size(); i++) {
                        String msg = msgStrings.get(i);
                        controller.addMessage(msg);
                    }
                    controller.addMessage("*** новые сообщения ***");
                }
            } catch (IOException e) {
                log.error("Файла для извлечения 100 строк с историей сообщений не существует " + e.getMessage());
            }
        }
    }


}
