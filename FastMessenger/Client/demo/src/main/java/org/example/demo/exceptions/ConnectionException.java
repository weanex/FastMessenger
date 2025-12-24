package org.example.demo.exceptions;

public class ConnectionException extends ApplicationException {
    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}