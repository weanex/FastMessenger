package org.example.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ChatClient extends Application {

    // Компоненты интерфейса
    private TextArea chatArea;
    private TextField messageField;
    private TextField usernameField;
    private TextField hostField;
    private TextField portField;
    private Button connectButton;
    private Button sendButton;
    private Label statusLabel;
    private Label userCountLabel;
    private ListView<String> usersListView;
    private Label usersCountInfoLabel; // Для хранения ссылки на Label в панели пользователей

    // Сетевое подключение
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    // Список пользователей
    private List<String> connectedUsers = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        VBox loginPane = createLoginPane();
        Scene scene = new Scene(loginPane, 400, 250);
        primaryStage.setTitle("Чат клиент - Вход");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> disconnect());
    }

    private VBox createLoginPane() {
        VBox loginPane = new VBox(15);
        loginPane.setPadding(new Insets(20));
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setStyle("-fx-background-color: #f5f5f5;");

        Label titleLabel = new Label("Подключение к чату");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setAlignment(Pos.CENTER);

        inputGrid.add(new Label("Имя:"), 0, 0);
        usernameField = new TextField();
        usernameField.setPromptText("Ваше имя");
        usernameField.setText("Гость" + (int) (Math.random() * 100));
        usernameField.setPrefWidth(200);
        inputGrid.add(usernameField, 1, 0);

        inputGrid.add(new Label("Сервер:"), 0, 1);
        hostField = new TextField();
        hostField.setPromptText("localhost");
        hostField.setText("localhost");
        hostField.setPrefWidth(200);
        inputGrid.add(hostField, 1, 1);

        inputGrid.add(new Label("Порт:"), 0, 2);
        portField = new TextField();
        portField.setPromptText("5555");
        portField.setText("5555");
        portField.setPrefWidth(200);
        inputGrid.add(portField, 1, 2);

        connectButton = new Button("Подключиться");
        connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        connectButton.setOnAction(e -> connectToServer());

        loginPane.getChildren().addAll(titleLabel, inputGrid, connectButton);
        return loginPane;
    }

    private void connectToServer() {
        String username = usernameField.getText().trim();
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();

        if (username.isEmpty() || host.isEmpty() || portText.isEmpty()) {
            showAlert("Ошибка", "Заполните все поля");
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("USERNAME:" + username);
            isConnected.set(true);
            createChatInterface(username);
            new Thread(this::readMessages).start();

        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Порт должен быть числом");
        } catch (IOException e) {
            showAlert("Ошибка подключения", "Не удалось подключиться к серверу");
            e.printStackTrace();
        }
    }

    private void createChatInterface(String username) {
        Stage chatStage = new Stage();
        BorderPane chatPane = new BorderPane();

        // Верхняя панель
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: #2c3e50;");

        Label userLabel = new Label("Вы: " + username);
        userLabel.setTextFill(Color.WHITE);

        statusLabel = new Label("✓ Подключен");
        statusLabel.setTextFill(Color.LIGHTGREEN);

        // Метка с количеством пользователей
        userCountLabel = new Label("Пользователей: 1");
        userCountLabel.setTextFill(Color.WHITE);
        userCountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button disconnectButton = new Button("Выйти");
        disconnectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        disconnectButton.setOnAction(e -> {
            disconnect();
            chatStage.close();
        });

        topPanel.getChildren().addAll(userLabel, userCountLabel, spacer, statusLabel, disconnectButton);
        chatPane.setTop(topPanel);

        // Основная область с разделителем
        SplitPane mainSplitPane = new SplitPane();

        // Левая панель со списком пользователей
        VBox usersPanel = createUsersPanel();

        // Правая панель с чатом
        VBox chatPanel = createChatPanel();

        mainSplitPane.getItems().addAll(usersPanel, chatPanel);
        mainSplitPane.setDividerPositions(0.2); // 20% для списка пользователей

        chatPane.setCenter(mainSplitPane);

        // Нижняя панель для ввода
        HBox bottomPanel = new HBox(10);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.setStyle("-fx-background-color: #ecf0f1;");

        messageField = new TextField();
        messageField.setPromptText("Введите сообщение...");
        messageField.setOnAction(e -> sendMessage());
        HBox.setHgrow(messageField, Priority.ALWAYS);

        sendButton = new Button("Отправить");
        sendButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        sendButton.setOnAction(e -> sendMessage());

        bottomPanel.getChildren().addAll(messageField, sendButton);
        chatPane.setBottom(bottomPanel);

        Scene chatScene = new Scene(chatPane, 800, 500);
        chatStage.setScene(chatScene);
        chatStage.setTitle("Чат - " + username);
        chatStage.show();
        chatStage.setOnCloseRequest(e -> disconnect());
    }

    private VBox createUsersPanel() {
        VBox usersPanel = new VBox();
        usersPanel.setPadding(new Insets(10));
        usersPanel.setSpacing(10);
        usersPanel.setStyle("-fx-background-color: #34495e;");

        // Заголовок панели пользователей
        Label usersTitle = new Label("Пользователи онлайн");
        usersTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        usersTitle.setTextFill(Color.WHITE);

        // Список пользователей
        usersListView = new ListView<>();
        usersListView.setStyle(
                "-fx-background-color: #2c3e50;" +
                        "-fx-text-fill: white;" +
                        "-fx-control-inner-background: #2c3e50;" +
                        "-fx-font-size: 13px;");
        usersListView.setPrefWidth(150);

        // Информация о количестве
        usersCountInfoLabel = new Label("Всего: 1"); // Сохраняем ссылку
        usersCountInfoLabel.setTextFill(Color.LIGHTGREEN);
        usersCountInfoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        usersPanel.getChildren().addAll(usersTitle, usersCountInfoLabel, usersListView);
        VBox.setVgrow(usersListView, Priority.ALWAYS);

        return usersPanel;
    }

    private VBox createChatPanel() {
        VBox chatPanel = new VBox();

        // Область чата
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle(
                "-fx-font-family: 'Arial';" +
                        "-fx-font-size: 14px;" +
                        "-fx-control-inner-background: #f8f9fa;");

        ScrollPane scrollPane = new ScrollPane(chatArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        chatPanel.getChildren().add(scrollPane);
        return chatPanel;
    }

    private void readMessages() {
        try {
            String message;
            while (isConnected.get() && (message = reader.readLine()) != null) {
                final String finalMessage = message;
                Platform.runLater(() -> {
                    if (finalMessage.startsWith("HISTORY:")) {
                        String history = finalMessage.substring(8);
                        String[] historyMessages = history.split("\\|");
                        for (String msg : historyMessages) {
                            if (!msg.isEmpty()) {
                                chatArea.appendText(msg + "\n");
                            }
                        }
                        chatArea.appendText("--- Начало текущей сессии ---\n");
                    } else if (finalMessage.contains("USERS_COUNT:")) {
                        // Обработка обновления количества пользователей
                        handleUsersUpdate(finalMessage);
                    } else {
                        chatArea.appendText(finalMessage + "\n");
                    }
                    chatArea.setScrollTop(Double.MAX_VALUE);
                });
            }
        } catch (IOException e) {
            if (isConnected.get()) {
                Platform.runLater(() -> {
                    statusLabel.setText("✗ Отключен");
                    statusLabel.setTextFill(Color.RED);
                    showAlert("Соединение потеряно", "Соединение с сервером разорвано");
                });
            }
        } finally {
            isConnected.set(false);
        }
    }

    private void handleUsersUpdate(String message) {
        // Формат: USERS_COUNT:количество:user1,user2,user3
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            int count = Integer.parseInt(parts[1]);
            String usersString = parts[2];

            // Обновляем метку с количеством
            userCountLabel.setText("Пользователей: " + count);

            // Обновляем список пользователей
            connectedUsers.clear();
            if (!usersString.isEmpty()) {
                String[] users = usersString.split(",");
                for (String user : users) {
                    if (!user.trim().isEmpty()) {
                        connectedUsers.add(user.trim());
                    }
                }
            }

            // Обновляем ListView
            usersListView.getItems().clear();
            usersListView.getItems().addAll(connectedUsers);

            // Обновляем информацию о количестве в панели пользователей
            updateUsersPanelCount(count);
        }
    }

    private void updateUsersPanelCount(int count) {
        Platform.runLater(() -> {
            // Обновляем метку в верхней панели
            userCountLabel.setText("Пользователей: " + count);

            // Обновляем метку в панели пользователей
            if (usersCountInfoLabel != null) {
                usersCountInfoLabel.setText("Всего: " + count);
            }
        });
    }

    private void sendMessage() {
        if (!isConnected.get() || writer == null) {
            showAlert("Ошибка", "Нет подключения к серверу");
            return;
        }

        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            writer.println("MESSAGE:" + message);
            messageField.clear();
            messageField.requestFocus();
        }
    }

    private void disconnect() {
        isConnected.set(false);

        try {
            if (writer != null) {
                writer.println("DISCONNECT");
                writer.close();
            }
            if (reader != null)
                reader.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            // Игнорируем ошибки при закрытии
        }

        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("✗ Отключен");
                statusLabel.setTextFill(Color.RED);
                userCountLabel.setText("Пользователей: 0");
                usersListView.getItems().clear();
            }
        });
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}