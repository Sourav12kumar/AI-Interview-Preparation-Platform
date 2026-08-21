package com.sourav.interviewprep.evaluation.ai;

import com.sourav.interviewprep.interview.entity.Difficulty;
import com.sourav.interviewprep.interview.entity.QuestionType;

import java.util.List;

public record AnswerEvaluationContext(
        String targetRole,
        Difficulty difficulty,
        String questionText,
        QuestionType questionType,
        List<String> expectedTopics,
        String answerText
) {
}
