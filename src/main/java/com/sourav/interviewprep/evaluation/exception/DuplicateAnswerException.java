package com.sourav.interviewprep.evaluation.exception;

public class DuplicateAnswerException extends RuntimeException {
    public DuplicateAnswerException() { super("This interview question has already been answered"); }
}
