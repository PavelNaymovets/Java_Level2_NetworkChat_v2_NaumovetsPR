package ru.gb.networkchat_v2.server;

import java.io.Closeable;

public interface UsernameService extends Closeable {
    boolean changeUsername(String login, String newUserName);
}
