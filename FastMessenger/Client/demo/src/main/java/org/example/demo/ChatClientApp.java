package org.example.demo;

import javafx.application.Application;
import javafx.stage.Stage;

public class ChatClientApp extends Application {
    private ChatController controller;

    @Override
    public void start(Stage primaryStage) {
        controller = new ChatController();
        controller.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}