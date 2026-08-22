package com.sourav.interviewprep.admin.exception;

public class DuplicateCodingProblemException extends RuntimeException {
    public DuplicateCodingProblemException() {
        super("A coding problem with this slug already exists");
    }
}
