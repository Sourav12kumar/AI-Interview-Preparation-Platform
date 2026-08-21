package com.sourav.interviewprep.interview.exception;

public class InterviewNotFoundException extends RuntimeException {
    public InterviewNotFoundException() { super("Interview session was not found"); }
}
