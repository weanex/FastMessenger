package org.example.demo;

import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private List<String> connectedUsers;

    public UserManager() {
        this.connectedUsers = new ArrayList<>();
    }

    public void updateUsersFromServer(String message) {
        try {
            // Формат: USERS_COUNT:количество:user1,user2,user3
            String[] parts = message.split(":", 3);
            if (parts.length < 3) {
                System.err.println("Некорректный формат сообщения о пользователях: " + message);
                return;
            }
            
            String usersString = parts[2];
            connectedUsers.clear();
            
            if (!usersString.isEmpty()) {
                String[] users = usersString.split(",");
                for (String user : users) {
                    String trimmedUser = user.trim();
                    if (!trimmedUser.isEmpty()) {
                        connectedUsers.add(trimmedUser);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки списка пользователей: " + e.getMessage());
        }
    }

    public void clearUsers() {
        connectedUsers.clear();
    }

    public List<String> getConnectedUsers() {
        return new ArrayList<>(connectedUsers);
    }

    public int getUserCount() {
        return connectedUsers.size();
    }
}