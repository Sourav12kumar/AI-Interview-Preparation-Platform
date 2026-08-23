package com.sourav.interviewprep.coding.exception;

public class CodingProblemNotFoundException extends RuntimeException {
    public CodingProblemNotFoundException() {
        super("Coding problem not found");
    }
}
