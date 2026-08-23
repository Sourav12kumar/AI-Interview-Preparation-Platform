package com.sourav.interviewprep.interview.ai;

import com.sourav.interviewprep.interview.entity.QuestionType;

import java.util.List;

public record GeneratedQuestion(
        String questionText,
        QuestionType questionType,
        List<String> expectedTopics
) {
}
