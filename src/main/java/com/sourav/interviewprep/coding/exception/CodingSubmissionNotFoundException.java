package com.sourav.interviewprep.coding.exception;

public class CodingSubmissionNotFoundException extends RuntimeException {
    public CodingSubmissionNotFoundException() {
        super("Coding submission not found");
    }
}
