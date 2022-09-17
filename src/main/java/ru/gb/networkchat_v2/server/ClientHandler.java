package ru.gb.networkchat_v2.server;

import ru.gb.networkchat_v2.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static ru.gb.networkchat_v2.Command.*;

/*
    Класс, который будет работать с конкретным клиентом. То есть каждый раз будет создаваться экземпляр
    этого класса. И каждый клиент будет подключаться по своему сокету. Несколько клиентов могут коннектиться к одному серверу.

    Общение с конкретным клиентом происходит через этот класс
*/
public class ClientHandler {
    private static final int TIMEOUT_AUTHENTICATE = 120_000; //время ожидания аутентификации пользователя
    private Socket socket; //Точка соединения с сервером
    private ChatServer server;//Хранит всю информацию о клиентах
    private DataInputStream in;//Поток получения информации
    private DataOutputStream out;//Поток передачи информации
    private String nick;//Ник участника чата
    private String oldNick;//Ник до изменения ника
    private String login;//Логин участника чата
    private AuthService authService;//Аутентификация пользователя
    private UsernameService usernameService;
    //    private final int CONNECTION_TIME = 20_000; //Время на подклюечение к серверу
    private Thread timeoutThread;

    public ClientHandler(Socket socket, ChatServer server, AuthService authService, UsernameService usernameService) {
        try {
            this.server = server;
            this.socket = socket;
            this.authService = authService;
            this.usernameService = usernameService;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.timeoutThread = new Thread(() -> {
                try {
                    Thread.sleep(TIMEOUT_AUTHENTICATE);
                    sendMessage(FINISH);//Если в другом потоке не будет вызван метод interrupt, то мы попадем сюда
                } catch (InterruptedException e) {
                    System.out.println("Успешная авторизация");//В другом потоке была успешная авторизация
                }
            });
            timeoutThread.start();
            new Thread(() -> {
                try {
                    if (authenticate()) { //Аутентифицирую пользователя на стороне сервера
                        readMessage(); //Чтение сообщений от участников чата
                    }
                } finally {
                    closeConnection(); //Закрываю ресурсы, если участник чата вышел или прислал "/end"
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Соединение с сервером прервано! Перезапустите приложение");
            e.printStackTrace();
        }
    }

    //Аутентификация пользователя
    private boolean authenticate() { // условный формат данных для аутентификации: /auth login1 pass1
//        Thread thread = new Thread(() -> { //Поток, который отсчитывает время на подключение к серверу
//            long start = System.currentTimeMillis();
//            while (true) {
//                long result = System.currentTimeMillis() - start;
//                if (result == CONNECTION_TIME) {
//                    sendMessage(FINISH);
//                    break;
//                }
//            }
//        });
//        thread.setDaemon(true); //Делаем поток демоном, чтобы не ждать завершения его работы в случае удачной авторизации пользователя
//        thread.start(); //Запускаем поток
        while (true) {
            try {
                String authMessage = in.readUTF(); //Получаю сообщение от участница чата(клиента)
                Command command = Command.getCommand(authMessage);
                if(command == Command.END){
                    return false;
                }
                if (command == Command.AUTH) {
                    String[] params = command.parse(authMessage);
                    String login = params[0];//Логин из сообщение
                    String password = params[1];//Пароль из сообщения
                    String nick = authService.getNickByLoginAndPassword(login, password);//Ник по логину и паролю
                    if (nick != null) {//Занят ли ник
                        if (server.isNickBusy(nick)) {
                            sendMessage(Command.ERROR, "Пользователь уже авторизован");
                            continue;
                        }
                        this.timeoutThread.interrupt(); // при вызове этого метода у спящего треда будет брошено InterruptedException
                        sendMessage(Command.AUTHOK, nick); //Если ник не занят. Отправляю участнику чата(клиенту) сообщение для входа в чат
                        this.nick = nick;
                        this.login = login;
                        //Отправляем всем пользователям, что клиент подключился
                        server.broadcast(Command.MESSAGE, "Пользователь " + nick + " зашел в чат");
                        //Добавляем пользователя в список клиентов
                        server.subscribe(this);
                        return true;
                    } else {
                        sendMessage(Command.ERROR, "Неверные логин и пароль");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(Command command, String... params) {
        sendMessage(command.collectMessage(params));
    }

    //Закрываю подключение
    private void closeConnection() {
        sendMessage(END); //Отключаю участника от сервера
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
        while (true) {
            try {
                String receivedMessage = in.readUTF(); //Сервер читает сообщение от конкретного участника
                Command command = Command.getCommand(receivedMessage);
                if (command == END) {
                    break;
                }
                if (command == PRIVATE_MESSAGE) {
                    String[] params = command.parse(receivedMessage);
                    String nickTo = params[0];
                    String message = params[1];
                    server.sendPrivateMessage(nickTo, message, this);
                    continue;
                }
                if (command == CHANGE_USERNAME) {
                    String newNick = command.parse(receivedMessage)[0];
                    rename(newNick);//Обновляю ник пользователя в списке клиентов сервера
                    server.updateSubscribe(oldNick, nick);//Обновляю ник в списке авторизованных пользователей
                    continue;
                } else {
                    server.broadcast(Command.MESSAGE, nick + ": " + command.parse(receivedMessage)[0]); //Сервер рассылает сообщение всем уже авторизированным участникам чата
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void rename(String newNick) {
        if(usernameService.changeUsername(login, newNick)){
            oldNick = this.nick; //Запомнил старый ник
            setNick(newNick); //Изменил старый ник на новый
            server.broadcastClientsList();//Обновляю список клиентов на панели чата(правая сторона)
            server.broadcast(Command.MESSAGE, "Пользователь " + oldNick + " сменил ник на " + newNick);//Отправляем всем пользователям, что пользователь сменил ник
        } else{
            sendMessage(NICK_BUSY, "Ник занят другим пользователем");
        }
    }

    private void setNick(String newNick) {
        this.nick = newNick;
    }

//    private String getMessage(String receivedMessage) {
//        StringBuilder message = new StringBuilder();
//        String[] rmArr = receivedMessage.split("\\p{Blank}+");
//        for (int i = 2; i < rmArr.length; i++) {
//            message.append(rmArr[i] + " ");
//        }
//        return message.toString().trim();
//    }

    public String getNick() {
        return nick;
    }
}
