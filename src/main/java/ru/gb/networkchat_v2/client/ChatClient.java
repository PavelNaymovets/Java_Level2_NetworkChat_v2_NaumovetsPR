package ru.gb.networkchat_v2.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
//Класс описывает логику работы приложения на стороне клиента
public class ChatClient {
    private Socket socket; //Точка соединения с сервером
    private DataInputStream in; //Поток получения информации
    private DataOutputStream out; //Поток передачи информации
    private ChatController controller; //Экземпляр класса, который описывает поведение элементов пользовательского интерфейса. Нужен для взаимодействия с интерфейсом пользователя

    public ChatClient(ChatController controller){
        this.controller = controller;
    }

    public void openConnection() throws IOException {
        socket = new Socket("localhost", 8189); //Создаем точку подключения к серверу. В данном случае мой компьютер
        in = new DataInputStream(socket.getInputStream()); //Открываем поток получения информации
        out = new DataOutputStream(socket.getOutputStream()); //Открываем поток передачи инфомарции
        new Thread(() -> {
            try {
                waitAuth();//Ожидаю аутентификации пользователя
                readMessages(); //Читаю сообщения от других бользователей
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection(); //Закрываю соединение если "/end"
            }
        }).start();
    }

    private void waitAuth() throws IOException {
        while(true){
            String message = in.readUTF(); //Читаю ответ от сервера
            if(message.startsWith("/authok")){ //Если пользователь аутентифицирован, то вернется сообщение в формате authok nick1
                String[] authok = message.split("\\p{Blank}+"); //Разбиваю сообщение на части
                String nick = authok[1]; //Беру ник пользователя
                controller.setAuth(true); //Делаю блок чата видимым
                controller.addMessage("Успешная авторизация под ником " + nick); //Передаю сообщение в окно истории чата только для себя
                break;
            }
        }
    }

    //Закрываю ресурсы соединения с сервером
    private void closeConnection() {
        if(in != null){
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(out != null){
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Читаю сообщения от сервера
    private void readMessages() throws IOException {
        while(true){
            String receivedMessage = in.readUTF();
            if("/end".equals(receivedMessage)){ //Если пользователь захотел отключиться от чата, направляет "/end"
                controller.setAuth(false); //Скрываем блок чата. Показываем блок авторизации
                break;
            }
            controller.addMessage(receivedMessage); //Добавляю сообщение от сервера в окно истории чата
        }
    }
    //Отправить сообщение на сервер
    public void sendMessage(String sendMessage) {
        try {
            out.writeUTF(sendMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
