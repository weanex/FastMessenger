package org.example.demo.exceptions;

public class ServerException extends ApplicationException {
    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}