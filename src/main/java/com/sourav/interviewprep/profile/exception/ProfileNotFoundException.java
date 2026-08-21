package com.sourav.interviewprep.profile.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() {
        super("Candidate profile was not found");
    }
}
