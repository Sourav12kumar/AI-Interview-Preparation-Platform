package com.sourav.interviewprep.interview.exception;

public class AiGenerationException extends RuntimeException {
    public AiGenerationException(String message) { super(message); }
    public AiGenerationException(String message, Throwable cause) { super(message, cause); }
}
