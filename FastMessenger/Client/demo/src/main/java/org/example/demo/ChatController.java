package org.example.demo;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ChatController {
    private ChatView view;
    private NetworkClient networkClient;
    private UserManager userManager;

    public void start(Stage primaryStage) {
        try {
            view = new ChatView(this);
            userManager = new UserManager();
            view.showLoginScreen(primaryStage);
        } catch (Exception e) {
            showAlert("Ошибка запуска", "Не удалось запустить приложение: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void connectToServer(String username, String host, int port) {
        // Валидация
        if (username == null || username.trim().isEmpty()) {
            showAlert("Ошибка", "Имя пользователя не может быть пустым");
            return;
        }
        if (host == null || host.trim().isEmpty()) {
            showAlert("Ошибка", "Адрес сервера не может быть пустым");
            return;
        }
        if (port < 1 || port > 65535) {
            showAlert("Ошибка", "Некорректный номер порта (1-65535)");
            return;
        }

        try {
            // Создаем клиент
            networkClient = new NetworkClient(host, port);
            
            // Подключаемся
            if (networkClient.connect()) {
                // Отправляем имя пользователя
                networkClient.send("USERNAME:" + username);
                
                // Показываем интерфейс чата
                view.showChatInterface(username);
                
                // Запускаем чтение сообщений в отдельном потоке
                new Thread(this::startReadingMessages).start();
            } else {
                showAlert("Ошибка подключения", "Не удалось подключиться к серверу");
            }
        } catch (Exception e) {
            showAlert("Ошибка", "Ошибка при подключении: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startReadingMessages() {
        try {
            String message;
            while ((message = networkClient.receive()) != null) {
                final String msg = message;
                Platform.runLater(() -> handleServerMessage(msg));
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                if (networkClient.isConnected()) {
                    showAlert("Ошибка", "Соединение с сервером потеряно");
                    view.updateConnectionStatus(false);
                }
            });
        }
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("HISTORY:")) {
            view.displayHistory(message.substring(8));
        } else if (message.startsWith("USERS_COUNT:")) {
            userManager.updateUsersFromServer(message);
            view.updateUserList(userManager.getConnectedUsers());
            view.updateUserCount(userManager.getUserCount());
        } else {
            view.displayMessage(message);
        }
    }

    public void sendMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            showAlert("Ошибка", "Сообщение не может быть пустым");
            return;
        }
        
        if (networkClient == null || !networkClient.isConnected()) {
            showAlert("Ошибка", "Нет подключения к серверу");
            return;
        }

        try {
            networkClient.send("MESSAGE:" + message);
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось отправить сообщение: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (networkClient != null) {
            try {
                networkClient.send("DISCONNECT");
            } catch (Exception e) {
                // Игнорируем ошибки при отключении
            }
            networkClient.disconnect();
        }
        
        if (userManager != null) {
            userManager.clearUsers();
            view.updateUserList(userManager.getConnectedUsers());
            view.updateUserCount(0);
        }
        
        view.updateConnectionStatus(false);
    }

    public void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}