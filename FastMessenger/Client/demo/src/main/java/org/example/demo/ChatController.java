package org.example.demo;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ChatController {
    private ChatView view;
    private NetworkClient networkClient;
    private UserManager userManager;

    public void start(Stage primaryStage) {
        view = new ChatView(this);
        userManager = new UserManager(view);
        view.showLoginScreen(primaryStage);
    }

    public void connectToServer(String username, String host, int port) {
        networkClient = new NetworkClient(host, port, this::handleServerMessage);
        
        if (networkClient.connect()) {
            networkClient.sendMessage("USERNAME:" + username);
            view.showChatInterface(username);
            new Thread(networkClient::startReading).start();
        } else {
            Platform.runLater(() -> 
                showAlert("Ошибка подключения", "Не удалось подключиться к серверу")
            );
        }
    }

    public void sendMessage(String message) {
        if (networkClient != null && networkClient.isConnected()) {
            networkClient.sendMessage("MESSAGE:" + message);
        } else {
            showAlert("Ошибка", "Нет подключения к серверу");
        }
    }

    public void disconnect() {
        if (networkClient != null) {
            networkClient.disconnect();
        }
        userManager.clearUsers();
    }

    private void handleServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("HISTORY:")) {
                view.displayHistory(message.substring(8));
            } else if (message.contains("USERS_COUNT:")) {
                userManager.updateUsersFromServer(message);
            } else {
                view.displayMessage(message);
            }
        });
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

    public void updateConnectionStatus(boolean connected) {
        view.updateConnectionStatus(connected);
    }
}