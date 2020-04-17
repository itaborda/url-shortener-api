package com.itaborda.exception;

public class WorkerNotFoundException  extends Exception{

    public WorkerNotFoundException() { super();
    }

    public WorkerNotFoundException(String message) {
        super(message);
    }
}
