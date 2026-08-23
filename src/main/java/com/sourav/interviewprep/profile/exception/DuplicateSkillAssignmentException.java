package com.sourav.interviewprep.profile.exception;

public class DuplicateSkillAssignmentException extends RuntimeException {
    public DuplicateSkillAssignmentException() {
        super("Skill is already assigned to this profile");
    }
}
