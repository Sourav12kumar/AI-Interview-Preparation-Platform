package com.sourav.interviewprep.resume.exception;

public class ResumeValidationException extends RuntimeException {
    public ResumeValidationException(String message) {
        super(message);
    }

    public ResumeValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
