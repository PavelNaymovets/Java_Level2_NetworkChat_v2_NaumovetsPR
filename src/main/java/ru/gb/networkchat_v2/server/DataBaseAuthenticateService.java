package ru.gb.networkchat_v2.server;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataBaseAuthenticateService extends DataBaseConnection implements AuthService{

    private static class UserDataFromDataBase{
        private String nick;
        private String login;
        private String password;

        public UserDataFromDataBase(String nick, String login, String password){
            this.nick = nick;
            this.login = login;
            this.password = password;
        }

        public String getNick() {
            return nick;
        }

        public String getLogin(){
            return login;
        }

        public String getPassword(){
            return password;
        }

//        @Override
//        public String toString(){
//            return nick + " " + login + " " + password;
//        }
    }

    private List<UserDataFromDataBase> userDataFromDB;

    @Override
    public String getNickByLoginAndPassword(String login, String password) {
        return userDataFromDB.stream()
                .filter(user -> login.equals(user.getLogin()) && password.equals(user.getPassword()))
                .findFirst()
                .map(UserDataFromDataBase::getNick)
                .orElse(null);
    }

    public DataBaseAuthenticateService(){
        userDataFromDB = new ArrayList<>();
        try (ResultSet resultSet = getStatement().executeQuery("SELECT * FROM authenticate;")){
            while (resultSet.next()){
                String nick = resultSet.getString("Username");
                String login = resultSet.getString("Login");
                String password = resultSet.getString("Password");

                UserDataFromDataBase user = new UserDataFromDataBase(nick,login,password);
                userDataFromDB.add(user);
            }
            close();//закрываю соединение с базой
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

}
