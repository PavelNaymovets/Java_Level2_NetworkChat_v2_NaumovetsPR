package ru.gb.networkchat_v2.server;

import ru.gb.networkchat_v2.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.gb.networkchat_v2.Command.CLIENTS;

//Класс описывает логику работы сервера. Этот класс знает всю информацию об участниках чата(клиентах)
public class ChatServer {
    //Список клиентов
    private Map<String,ClientHandler> clients; //Список участников чата (клиентов сервера).

    public ChatServer() {
        this.clients = new HashMap<>();
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

    //Отправить сообщение конкретному пользователю
    public void sendPrivateMessage(String nickTo, String message, ClientHandler senderName){
        ClientHandler clientTo = clients.get(nickTo);
        if(clientTo == null){
            senderName.sendMessage(Command.ERROR, "Пользователь не авторизован!");
            return;
        }
        clientTo.sendMessage(Command.MESSAGE, "От " + senderName.getNick() + ": " + message);
        senderName.sendMessage(Command.MESSAGE,"Участнику " + nickTo + ": " + message);
    }
    //Добавление участника чата(клиента) в список клиентов(в дальнейшем применяем для общей рассылки).
    public void subscribe(ClientHandler client) {
        clients.put(client.getNick(), client);
        broadcastClientsList();
    }

    private void broadcastClientsList() {
        String nicks = clients.values().stream()
                .map(ClientHandler::getNick)
                .collect(Collectors.joining(" "));
        broadcast(CLIENTS, nicks);
    }

    //Рассылка сообщений всем участникам чата
    public void broadcast(Command command, String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(command, message);
        }
    }

    //Проверка занят ник или нет
    public boolean isNickBusy(String nick) {
        return clients.get(nick) != null;
    }

    //Удаление пользователя из списка клиентов, если он вышел из чата
    public void unsubscribe(ClientHandler client) {
        clients.remove(client.getNick());
        broadcastClientsList();
    }
}
