package ru.gb.networkchat_v2.server;
//Класс запускает сервер
public class ServerLauncher {
    public static void main(String[] args) {
        new ChatServer().start(); //Запускаем сервер. Метод .start()
    }
}
