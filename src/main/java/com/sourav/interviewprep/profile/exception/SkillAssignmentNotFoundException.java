package com.sourav.interviewprep.profile.exception;

public class SkillAssignmentNotFoundException extends RuntimeException {
    public SkillAssignmentNotFoundException() {
        super("Skill assignment was not found");
    }
}
