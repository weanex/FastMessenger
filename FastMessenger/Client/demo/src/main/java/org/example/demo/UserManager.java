package org.example.demo;

import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private List<String> connectedUsers;
    private ChatView view;

    public UserManager(ChatView view) {
        this.view = view;
        this.connectedUsers = new ArrayList<>();
    }

    public void updateUsersFromServer(String message) {
        // Формат: USERS_COUNT:количество:user1,user2,user3
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            int count = Integer.parseInt(parts[1]);
            String usersString = parts[2];

            connectedUsers.clear();
            if (!usersString.isEmpty()) {
                String[] users = usersString.split(",");
                for (String user : users) {
                    if (!user.trim().isEmpty()) {
                        connectedUsers.add(user.trim());
                    }
                }
            }

            view.updateUserList(connectedUsers);
            view.updateUserCount(count);
        }
    }

    public void clearUsers() {
        connectedUsers.clear();
        view.updateUserList(connectedUsers);
        view.updateUserCount(0);
    }

    public List<String> getConnectedUsers() {
        return new ArrayList<>(connectedUsers);
    }

    public int getUserCount() {
        return connectedUsers.size();
    }
}