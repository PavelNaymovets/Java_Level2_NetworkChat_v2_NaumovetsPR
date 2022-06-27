package ru.gb.networkchat_v2.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InMemoryAuthService implements AuthService {

    private static class UserData {
        private String nick;
        private String login;
        private String password;

        public UserData(String nick, String login, String password) {
            this.nick = nick;
            this.login = login;
            this.password = password;
        }

        public String getNick() {
            return nick;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }
    }

    private List<UserData> users;
    //Создание ников, логинов, паролей для участников чата
    public InMemoryAuthService(){
        users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(new UserData("nick" + i, "login" + i, "pass" + i));
        }
    }
    //Получение ника от логину и паролю участница
    @Override
    public String getNickByLoginAndPassword(String login, String password) {
        for (UserData user : users) {
            if(login.equals(user.getLogin()) && password.equals(user.getPassword())){
                return user.getNick();
            }
        }
        return null;
        /*
        Через стримы
        return users.stream()
                .filter(user -> login.equals(user.getLogin())
                        && password.equals(user.getPassword()))
                .findFirst()
                .map(UserData::getNick)
                .orElse(null);
         */
    }

    @Override
    public void close() throws IOException {
        System.out.println("Сервис аутентификации остановлен");
    }
}
