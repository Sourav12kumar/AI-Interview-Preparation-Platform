package com.sourav.interviewprep.interview.dto;

import com.sourav.interviewprep.interview.entity.Difficulty;
import com.sourav.interviewprep.interview.entity.QuestionType;

import java.util.List;

public record InterviewQuestionResponse(
        Long id,
        int sequenceNumber,
        String questionText,
        QuestionType questionType,
        Difficulty difficulty,
        List<String> expectedTopics,
        boolean aiGenerated
) {
}
