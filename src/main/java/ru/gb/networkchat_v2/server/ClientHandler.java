package ru.gb.networkchat_v2.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
/*
    Класс, который будет работать с конкретным клиентом. То есть каждый раз будет создаваться экземпляр
    этого класса. И каждый клиент будет подключаться по своему сокету. Несколько клиентов могут коннектиться к одному серверу.

    Общение с конкретным клиентом происходит через этот класс
*/
public class ClientHandler {
    private Socket socket; //Точка соединения с сервером
    private ChatServer server;//Хранит всю информацию о клиентах
    private DataInputStream in;//Поток получения информации
    private DataOutputStream out;//Поток передачи информации
    private String nick;//Ник участника чата
    private AuthService authService;//Аутентификация пользователя

    public ClientHandler(Socket socket, ChatServer server, AuthService authService) {
        try {
            this.server = server;
            this.socket = socket;
            this.authService = authService;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try{
                    authenticate(); //Аутентифицирую пользователя на стороне сервера
                    readMessage(); //Чтение сообщений от участников чата
                } finally {
                    closeConnection(); //Закрываю ресурсы, если участник чата вышел или прислал "/end"
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Аутентификация пользователя
    private void authenticate() { // условный формат данных для аутентификации: /auth login1 pass1
        while(true){
            try {
                String authMessage = in.readUTF(); //Получаю сообщение от участница чата(клиента)
                if(authMessage.startsWith("/auth")){ //Пришло сообщение с пометкой "/auth"
                    String[] logPass = authMessage.split("\\p{Blank}+");
                    String login = logPass[1];//Логин из сообщение
                    String password = logPass[2];//Пароль из сообщения
                    String nick = authService.getNickByLoginAndPassword(login, password);//Ник по логину и паролю
                    if(nick != null){//Занят ли ник
                        if(server.isNickBusy(nick)){
                            sendMessage("Пользователь уже авторизован");
                            continue;
                        }
                        sendMessage("/authok " + nick); //Если ник не занят. Отправляю участнику чата(клиенту) сообщение для входа в чат
                        this.nick = nick;
                        //Отправляем всем пользователям, что клиент подключился
                        server.broadcast("Пользователь " + nick + " зашел в чат");
                        //Добавляем пользователя в список клиентов
                        server.subscribe(this);
                        break;
                    } else{
                        sendMessage("Неверные логин и пароль");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //Закрываю подключение
    private void closeConnection() {
        sendMessage("/end"); //Отключаю участника от сервера
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
            server.unsubscribe(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //Отправка сообщений от сервера к участникам чата
    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Чтение сообщений от участников чата и рассылка этих сообщений всем участникам чата
    private void readMessage() {
        while(true){
            try {
                String receivedMessage = in.readUTF(); //Сервер читает сообщение от конкретного участника
                if("/end".equals(receivedMessage)){
                    break;
                }
                if("/w".startsWith(receivedMessage)){ //Отпарвка сообщения конкретному пользователю
                    String[] oneUser = receivedMessage.split("\\p{Blank}+"); //Разбиваю сообщение на части
                    String nickUser = oneUser[1];//Ник из сообщения
                    String messageUser = oneUser[2];//Личное сообщение от одного пользователя для другого
                    server.sendMessSpecChatParticipant(nickUser, messageUser);//Отправляю личное сообщение пользователю по нику
                } else{
                    server.broadcast(nick + ": " + receivedMessage); //Сервер рассылает сообщение всем уже авторизированным участникам чата
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getNick() {
        return nick;
    }
}
