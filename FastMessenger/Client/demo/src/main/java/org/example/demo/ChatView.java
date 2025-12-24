package org.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.List;

public class ChatView {
    private ChatController controller;
    
    // UI компоненты
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
    private Label usersCountInfoLabel;
    private Stage primaryStage;
    
    public ChatView(ChatController controller) {
        this.controller = controller;
    }

    public void showLoginScreen(Stage stage) {
        this.primaryStage = stage;
        
        VBox loginPane = createLoginPane();
        Scene scene = new Scene(loginPane, 400, 250);
        
        stage.setTitle("Чат клиент - Вход");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(e -> controller.disconnect());
    }

    public void showChatInterface(String username) {
        // Закрываем окно логина
        if (primaryStage != null) {
            primaryStage.close();
        }
        
        Stage chatStage = new Stage();
        BorderPane chatPane = createMainChatPane(username);
        Scene chatScene = new Scene(chatPane, 800, 500);
        
        chatStage.setTitle("Чат - " + username);
        chatStage.setScene(chatScene);
        chatStage.show();
        chatStage.setOnCloseRequest(e -> controller.disconnect());
    }

    private VBox createLoginPane() {
        VBox loginPane = new VBox(15);
        loginPane.setPadding(new Insets(20));
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setStyle("-fx-background-color: #f5f5f5;");

        Label titleLabel = new Label("Подключение к чату");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        GridPane inputGrid = createLoginInputGrid();
        connectButton = createConnectButton();

        loginPane.getChildren().addAll(titleLabel, inputGrid, connectButton);
        return loginPane;
    }

    private GridPane createLoginInputGrid() {
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setAlignment(Pos.CENTER);

        usernameField = new TextField();
        usernameField.setPromptText("Ваше имя");
        usernameField.setText("Гость" + (int)(Math.random() * 1000));
        usernameField.setPrefWidth(200);

        hostField = new TextField();
        hostField.setPromptText("localhost");
        hostField.setText("localhost");
        hostField.setPrefWidth(200);

        portField = new TextField();
        portField.setPromptText("5555");
        portField.setText("5555");
        portField.setPrefWidth(200);

        inputGrid.add(new Label("Имя:"), 0, 0);
        inputGrid.add(usernameField, 1, 0);
        inputGrid.add(new Label("Сервер:"), 0, 1);
        inputGrid.add(hostField, 1, 1);
        inputGrid.add(new Label("Порт:"), 0, 2);
        inputGrid.add(portField, 1, 2);

        return inputGrid;
    }

    private Button createConnectButton() {
        Button button = new Button("Подключиться");
        button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        button.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();

            if (username.isEmpty() || host.isEmpty() || portText.isEmpty()) {
                showAlert("Ошибка", "Заполните все поля");
                return;
            }

            try {
                int port = Integer.parseInt(portText);
                controller.connectToServer(username, host, port);
            } catch (NumberFormatException ex) {
                showAlert("Ошибка", "Порт должен быть числом");
            }
        });
        return button;
    }

    private BorderPane createMainChatPane(String username) {
        BorderPane chatPane = new BorderPane();
        
        chatPane.setTop(createTopPanel(username));
        chatPane.setCenter(createMainSplitPane());
        chatPane.setBottom(createBottomPanel());
        
        return chatPane;
    }

    private HBox createTopPanel(String username) {
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: #2c3e50;");

        Label userLabel = new Label("Вы: " + username);
        userLabel.setTextFill(Color.WHITE);

        statusLabel = new Label("✓ Подключен");
        statusLabel.setTextFill(Color.LIGHTGREEN);

        userCountLabel = new Label("Пользователей: 1");
        userCountLabel.setTextFill(Color.WHITE);
        userCountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button disconnectButton = new Button("Выйти");
        disconnectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        disconnectButton.setOnAction(e -> controller.disconnect());

        topPanel.getChildren().addAll(userLabel, userCountLabel, spacer, statusLabel, disconnectButton);
        return topPanel;
    }

    private SplitPane createMainSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(createUsersPanel(), createChatPanel());
        splitPane.setDividerPositions(0.2);
        return splitPane;
    }

    private VBox createUsersPanel() {
        VBox usersPanel = new VBox();
        usersPanel.setPadding(new Insets(10));
        usersPanel.setSpacing(10);
        usersPanel.setStyle("-fx-background-color: #34495e;");

        Label usersTitle = new Label("Пользователи онлайн");
        usersTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        usersTitle.setTextFill(Color.WHITE);

        usersListView = new ListView<>();
        usersListView.setStyle(
                "-fx-background-color: #2c3e50;" +
                "-fx-text-fill: white;" +
                "-fx-control-inner-background: #2c3e50;" +
                "-fx-font-size: 13px;");
        usersListView.setPrefWidth(150);

        usersCountInfoLabel = new Label("Всего: 1");
        usersCountInfoLabel.setTextFill(Color.LIGHTGREEN);
        usersCountInfoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        usersPanel.getChildren().addAll(usersTitle, usersCountInfoLabel, usersListView);
        VBox.setVgrow(usersListView, Priority.ALWAYS);

        return usersPanel;
    }

    private VBox createChatPanel() {
        VBox chatPanel = new VBox();
        
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

    private HBox createBottomPanel() {
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
        return bottomPanel;
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            controller.sendMessage(message);
            messageField.clear();
            messageField.requestFocus();
        }
    }

    public void displayMessage(String message) {
        chatArea.appendText(message + "\n");
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    public void displayHistory(String history) {
        String[] historyMessages = history.split("\\|");
        for (String msg : historyMessages) {
            if (!msg.isEmpty()) {
                chatArea.appendText(msg + "\n");
            }
        }
        chatArea.appendText("--- Начало текущей сессии ---\n");
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    public void updateConnectionStatus(boolean connected) {
        if (connected) {
            statusLabel.setText("✓ Подключен");
            statusLabel.setTextFill(Color.LIGHTGREEN);
        } else {
            statusLabel.setText("✗ Отключен");
            statusLabel.setTextFill(Color.RED);
            userCountLabel.setText("Пользователей: 0");
            if (usersCountInfoLabel != null) {
                usersCountInfoLabel.setText("Всего: 0");
            }
        }
    }

    public void updateUserList(List<String> users) {
        if (usersListView != null) {
            usersListView.getItems().clear();
            usersListView.getItems().addAll(users);
        }
    }

    public void updateUserCount(int count) {
        if (userCountLabel != null) {
            userCountLabel.setText("Пользователей: " + count);
        }
        if (usersCountInfoLabel != null) {
            usersCountInfoLabel.setText("Всего: " + count);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}