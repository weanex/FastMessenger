package org.example.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class NetworkClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected;
    private String host;
    private int port;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.connected = false;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(5000); // Таймаут 5 секунд
            
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            connected = true;
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка подключения к " + host + ":" + port + ": " + e.getMessage());
            return false;
        }
    }

    public void send(String message) throws IOException {
        if (!connected || writer == null) {
            throw new IOException("Соединение не установлено");
        }
        writer.println(message);
    }

    public String receive() throws IOException {
        if (!connected || reader == null) {
            throw new IOException("Соединение не установлено");
        }
        
        try {
            return reader.readLine();
        } catch (SocketTimeoutException e) {
            // Таймаут - это нормально, продолжаем слушать
            return "";
        }
    }

    public void disconnect() {
        connected = false;
        
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}