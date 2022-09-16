package ru.gb.networkchat_v2.server;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseConnection implements Closeable {

    private static Connection connection; //соединение  с базой
    private static Statement stmt; //SQL запрос
    private static final String DATA_BASE_PATH = "C:\\Users\\Павел\\IdeaProjects\\NetworkChat_v2\\src\\main\\resources\\ru.gb.networkchat_v2.server\\authenticate.db";


    //Создаю соединение с базой
    public DataBaseConnection() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DATA_BASE_PATH);
            stmt = connection.createStatement();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось подключиться к базе данных: " + e.getMessage(), e);
        }
    }

    //Закрываю соединение с базой
    @Override
    public void close() throws IOException {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Возвращаю объект типа Statement для выполнения запроса в базу
    protected Statement getStatement() {
        return stmt;
    }
    //Возвращаю соединение с базой
    protected Connection getConnection(){
        return connection;
    }
}
