ALTER TABLE interview_answers
    ADD CONSTRAINT chk_answers_response_time
        CHECK (response_time_seconds IS NULL OR response_time_seconds BETWEEN 0 AND 7200);

ALTER TABLE answer_evaluations
    ADD CONSTRAINT chk_evaluations_technical_score
        CHECK (technical_score IS NULL OR technical_score BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_evaluations_relevance_score
        CHECK (relevance_score IS NULL OR relevance_score BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_evaluations_clarity_score
        CHECK (clarity_score IS NULL OR clarity_score BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_evaluations_confidence_score
        CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_evaluations_overall_score
        CHECK (overall_score BETWEEN 0 AND 100);
