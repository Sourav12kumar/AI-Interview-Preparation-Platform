package com.sourav.interviewprep.coding.exception;

public class CodeRunnerUnavailableException extends RuntimeException {
    public CodeRunnerUnavailableException(String message) {
        super(message);
    }

    public CodeRunnerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
