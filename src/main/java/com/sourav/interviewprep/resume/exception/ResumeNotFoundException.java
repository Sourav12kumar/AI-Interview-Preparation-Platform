package com.sourav.interviewprep.resume.exception;

public class ResumeNotFoundException extends RuntimeException {
    public ResumeNotFoundException() {
        super("Resume not found");
    }
}
