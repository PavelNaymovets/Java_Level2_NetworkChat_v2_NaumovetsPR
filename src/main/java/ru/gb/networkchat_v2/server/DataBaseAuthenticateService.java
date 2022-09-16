package ru.gb.networkchat_v2.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataBaseAuthenticateService extends DataBaseConnection implements AuthService, UsernameService {

    private static class UserDataFromDataBase {
        private String nick;
        private String login;
        private String password;

        public UserDataFromDataBase(String nick, String login, String password) {
            this.nick = nick;
            this.login = login;
            this.password = password;
        }

        public String getNick() {
            return nick;
        }
        public void setNick(String newUserName){
            this.nick = newUserName;
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

    private static List<UserDataFromDataBase> userDataFromDB;

    @Override
    public String getNickByLoginAndPassword(String login, String password) {
        return userDataFromDB.stream()
                .filter(user -> login.equals(user.getLogin()) && password.equals(user.getPassword()))
                .findFirst()
                .map(UserDataFromDataBase::getNick)
                .orElse(null);
    }

    public DataBaseAuthenticateService() {
        userDataFromDB = new ArrayList<>();
        getDataUsersFromDB(userDataFromDB);
    }

    private void getDataUsersFromDB(List<UserDataFromDataBase> userDataFromDB) {
        try (ResultSet resultSet = getStatement().executeQuery("SELECT * FROM authenticate;")) {
            while (resultSet.next()) {
                String nick = resultSet.getString("Username");
                String login = resultSet.getString("Login");
                String password = resultSet.getString("Password");

                UserDataFromDataBase user = new UserDataFromDataBase(nick, login, password);
                userDataFromDB.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean changeUsername(String login, String newUserName) {
        try {
            getDataUsersFromDB(userDataFromDB);//Считаю базу. Проверка внесенных кем-то измненений.
            for (UserDataFromDataBase user : userDataFromDB) {
                if (user.getNick().equalsIgnoreCase(newUserName)) {//Если ник занят верну false
                    return false;
                }
            }
            PreparedStatement prs = getConnection().prepareStatement("UPDATE authenticate SET username = ? WHERE login = ?;");
            prs.setString(1, newUserName);
            prs.setString(2, login);
            prs.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < userDataFromDB.size(); i++) { //обновляю ник в считаной мною базе
            if(userDataFromDB.get(i).getLogin().equalsIgnoreCase(login)){
                userDataFromDB.get(i).setNick(newUserName);
            }
        }
        return true;
    }

}
