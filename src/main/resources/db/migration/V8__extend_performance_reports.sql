ALTER TABLE performance_reports
    ADD COLUMN analysis_summary TEXT NULL AFTER ai_recommendations,
    ADD COLUMN ai_model VARCHAR(80) NULL AFTER analysis_summary,
    ADD COLUMN prompt_version VARCHAR(30) NULL AFTER ai_model,
    ADD CONSTRAINT chk_reports_period CHECK (period_start <= period_end),
    ADD CONSTRAINT chk_reports_interview_count CHECK (interviews_completed >= 0),
    ADD CONSTRAINT chk_reports_coding_count CHECK (coding_problems_attempted >= 0),
    ADD CONSTRAINT chk_reports_interview_score
        CHECK (average_interview_score IS NULL OR (average_interview_score >= 0 AND average_interview_score <= 100)),
    ADD CONSTRAINT chk_reports_coding_score
        CHECK (average_coding_score IS NULL OR (average_coding_score >= 0 AND average_coding_score <= 100));

CREATE INDEX idx_reports_user_generated
    ON performance_reports (user_id, generated_at);
