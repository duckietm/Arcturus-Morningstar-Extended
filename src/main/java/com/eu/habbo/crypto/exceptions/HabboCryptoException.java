package com.eu.habbo.crypto.exceptions;

public class HabboCryptoException extends Exception {

    public HabboCryptoException(String message) {
        super(message);
    }

    public HabboCryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    public HabboCryptoException(Throwable cause) {
        super(cause);
    }

}
