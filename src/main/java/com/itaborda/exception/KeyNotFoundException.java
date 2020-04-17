package com.itaborda.exception;

public class KeyNotFoundException extends Exception{

    public KeyNotFoundException() { super();
    }

    public KeyNotFoundException(String message) {
        super(message);
    }
}
