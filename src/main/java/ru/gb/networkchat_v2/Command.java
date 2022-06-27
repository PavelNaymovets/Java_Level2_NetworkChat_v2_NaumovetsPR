package ru.gb.networkchat_v2;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

//Класс хранит константы, которые применены в коде
public enum Command {
    //Item явяется наследником Command (как бы абстрактного класса). Должен имплементировать абстрактный метод .parse()
    AUTH("/auth") { //аутентификация
        @Override
        public String[] parse(String commandText) {//Текста команды(commandText) //auth login1 pass1
            String[] message = commandText.split(TOKEN_DELIMITER);
            return new String[] {message[1], message[2]};//Возвращается логин и пароль
        }
    },
    AUTHOK("/authok"){ //аутентификация пройдена
        @Override
        public String[] parse(String commandText) { //authok nick1
            String[] message = commandText.split(TOKEN_DELIMITER);
            return new String[]{message[1]};//Возвращается ник
        }
    },
    END("/end"){ //сообщение о завершении работы
        @Override
        public String[] parse(String commandText) {
            return new String[0];
        }
    },
    PRIVATE_MESSAGE("/w"){//отправка приватного сообщения
        @Override
        public String[] parse(String commandText) {// /w nick1 textMessage
            String[] message = commandText.split(TOKEN_DELIMITER, 3);//указываем разделитель и количество участков деления
            return new String[]{message[1], message[2]};
        }
    },
    CLIENTS("/clients"){ //возврат списка клиентов
        @Override
        public String[] parse(String commandText) {// /clients nick1 nick2 nick3
            String[] message = commandText.split(TOKEN_DELIMITER);
            String[] nicks = new String[message.length - 1];
            for (int i = 0; i < nicks.length; i++) {
                nicks[i] = message[i + 1];
            }
            return nicks;
        }
    },
    ERROR("/error"){ //сообщение об ошибке
        @Override
        public String[] parse(String commandText) {//error error message
            String[] message = commandText.split(TOKEN_DELIMITER,2);
            return new String[]{message[1]};
        }
    },

    MESSAGE("/message"){
        @Override
        public String[] parse(String commandText) {
            String[] message = commandText.split(TOKEN_DELIMITER,2);
            return new String[]{message[1]};
        }
    },

    FINISH("/finish") {
        @Override
        public String[] parse(String commandText) {
            return new String[0];
        }
    };

    private final String command;//Хранит обозначение команды
    private static final String TOKEN_DELIMITER = "\\p{Blank}+";//Регулярное выражение для разделения строки по пробелам
    private static final Map<String,Command> commandMap = Arrays.stream(values())
            .collect(Collectors.toMap(Command::getCommand, Function.identity()));

    //Конструктор для передачи параметра в энам
    Command(String command){
        this.command = command;
    }

    public String getCommand() {//Получаем текстовое значение команды(то, что указано в скобках)
        return command;
    }

    public static boolean isCommand(String message){//Проверяем является ли присланное сообщение командой. Команда, если начинается на "/".
        return message.startsWith("/");
    }

    public static Command getCommand(String message){ // проверяем является ли командой присланное сообщение или нет
        if(!isCommand(message)){ // /w nick1 Hello
            throw new RuntimeException("'" + message + "' is not a command");
        }
        String cmd = message.split(TOKEN_DELIMITER, 2)[0];
        Command command = commandMap.get(cmd);
        if(command == null){
            throw new RuntimeException("Unknown command '" + cmd + "'");
        }
        return command;
    }
    public abstract String[] parse(String commandText);

    public String collectMessage(String... params){//присоединить к константе какое-то сообщение
        return this.command + " " + String.join(" ", params);
    }
}
