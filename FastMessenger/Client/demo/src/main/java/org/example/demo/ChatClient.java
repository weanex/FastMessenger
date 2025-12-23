package org.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // Сетевое подключение
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    @Override
    public void start(Stage primaryStage) {
        // Создаем интерфейс входа
        VBox loginPane = createLoginPane();

        Scene scene = new Scene(loginPane, 400, 250);

        primaryStage.setTitle("Чат клиент - Вход");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Обработка закрытия окна
        primaryStage.setOnCloseRequest(e -> disconnect());
    }

    private VBox createLoginPane() {
        VBox loginPane = new VBox(15);
        loginPane.setPadding(new Insets(20));
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setStyle("-fx-background-color: #f5f5f5;");

        // Заголовок
        Label titleLabel = new Label("Подключение к чату");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Поля ввода
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setAlignment(Pos.CENTER);

        // Имя пользователя
        inputGrid.add(new Label("Имя:"), 0, 0);
        usernameField = new TextField();
        usernameField.setPromptText("Ваше имя");
        usernameField.setText("Гость" + (int)(Math.random() * 100));
        usernameField.setPrefWidth(200);
        inputGrid.add(usernameField, 1, 0);

        // Адрес сервера
        inputGrid.add(new Label("Сервер:"), 0, 1);
        hostField = new TextField();
        hostField.setPromptText("localhost");
        hostField.setText("localhost");
        hostField.setPrefWidth(200);
        inputGrid.add(hostField, 1, 1);

        // Порт
        inputGrid.add(new Label("Порт:"), 0, 2);
        portField = new TextField();
        portField.setPromptText("5555");
        portField.setText("5555");
        portField.setPrefWidth(200);
        inputGrid.add(portField, 1, 2);

        // Кнопка подключения
        connectButton = new Button("Подключиться");
        connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        connectButton.setOnAction(e -> connectToServer());

        // Добавляем все в панель
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

            // Подключаемся к серверу
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Отправляем имя пользователя
            writer.println("USERNAME:" + username);

            isConnected.set(true);

            // Создаем интерфейс чата
            createChatInterface(username);

            // Запускаем поток для чтения сообщений
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button disconnectButton = new Button("Выйти");
        disconnectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        disconnectButton.setOnAction(e -> {
            disconnect();
            chatStage.close();
        });

        topPanel.getChildren().addAll(userLabel, spacer, statusLabel, disconnectButton);
        chatPane.setTop(topPanel);

        // Область чата
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle(
                "-fx-font-family: 'Arial';" +
                        "-fx-font-size: 14px;" +
                        "-fx-control-inner-background: #f8f9fa;"
        );

        ScrollPane scrollPane = new ScrollPane(chatArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        chatPane.setCenter(scrollPane);

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

        // Создаем сцену и показываем окно
        Scene chatScene = new Scene(chatPane, 600, 400);
        chatStage.setScene(chatScene);
        chatStage.setTitle("Чат - " + username);
        chatStage.show();

        // Обработка закрытия окна чата
        chatStage.setOnCloseRequest(e -> disconnect());
    }

    private void readMessages() {
        try {
            String message;
            while (isConnected.get() && (message = reader.readLine()) != null) {
                final String finalMessage = message;
                Platform.runLater(() -> {
                    if (finalMessage.startsWith("HISTORY:")) {
                        // Обработка истории
                        String history = finalMessage.substring(8);
                        String[] historyMessages = history.split("\\|");
                        for (String msg : historyMessages) {
                            if (!msg.isEmpty()) {
                                chatArea.appendText(msg + "\n");
                            }
                        }
                        chatArea.appendText("--- Начало текущей сессии ---\n");
                    } else {
                        // Обычное сообщение
                        chatArea.appendText(finalMessage + "\n");
                    }
                    // Автопрокрутка вниз
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
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Игнорируем ошибки при закрытии
        }

        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("✗ Отключен");
                statusLabel.setTextFill(Color.RED);
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

    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}