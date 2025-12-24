package org.example.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private AtomicBoolean isConnected;
    private Consumer<String> messageHandler;
    private String host;
    private int port;

    public NetworkClient(String host, int port, Consumer<String> messageHandler) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
        this.isConnected = new AtomicBoolean(false);
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            isConnected.set(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void startReading() {
        try {
            String message;
            while (isConnected.get() && (message = reader.readLine()) != null) {
                messageHandler.accept(message);
            }
        } catch (IOException e) {
            if (isConnected.get()) {
                messageHandler.accept("Ошибка соединения с сервером");
            }
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        if (writer != null && isConnected.get()) {
            writer.println(message);
        }
    }

    public void disconnect() {
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
    }

    public boolean isConnected() {
        return isConnected.get();
    }
}