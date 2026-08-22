package com.sourav.interviewprep.coding.exception;

public class CodeExecutionException extends RuntimeException {
    public CodeExecutionException(String message) {
        super(message);
    }

    public CodeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
