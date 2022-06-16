package ru.gb.networkchat_v2.server;

import java.io.Closeable;
//Контракт взаимодействия для определения пользователя (логин, пароль)
public interface AuthService extends Closeable {
    String getNickByLoginAndPassword(String login, String password);
}
