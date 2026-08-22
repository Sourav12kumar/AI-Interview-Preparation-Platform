package com.sourav.interviewprep.coding.entity;

public enum SubmissionVerdict {
    QUEUED,
    RUNNING,
    ACCEPTED,
    WRONG_ANSWER,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    TIME_LIMIT_EXCEEDED;

    public boolean isFinal() {
        return this != QUEUED && this != RUNNING;
    }
}
