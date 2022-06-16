package ru.gb.networkchat_v2.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
//Класс описывает логику работы сервера. Этот класс знает всю информацию об участниках чата(клиентах)
public class ChatServer {
    //Список клиентов
    private List<ClientHandler> clients; //Список участников чата (клиентов сервера).

    public ChatServer() {
        this.clients = new ArrayList<>();
    }
    //Запуск сервера
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(8189); //Создаем серверный сокет
             AuthService authService = new InMemoryAuthService()) { //Создаем экземпляр класса определения пользователя
            while (true) {
                System.out.println("Ожидаю поключения...");
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this, authService); //Создаем экземпляр класса, описывающего взаимодействие сервера с каждым клиентом(уачстником чата)
                System.out.println("Клиент подключился");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Рассылка сообщений всем участникам чата
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    //Отправить сообщение конкретному пользователю
    public void sendMessSpecChatParticipant(String nick, String message){
        for (ClientHandler client : clients) {
            if(nick.equals(client.getNick())){
                client.sendMessage(message);
            }
        }
    }
    //Добавление участника чата(клиента) в список клиентов(в дальнейшем применяем для общей рассылки).
    public void subscribe(ClientHandler client) {
        clients.add(client);
    }
    //Проверка занят ник или нет
    public boolean isNickBusy(String nick) {
        for (ClientHandler client : clients) {
            if(nick.equals(client.getNick())){
                return true;
            }
        }
        return false;
    }

    //Удаление пользователя из списка клиентов, если он вышел из чата
    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }
}
