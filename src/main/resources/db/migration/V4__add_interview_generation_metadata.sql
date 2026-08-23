ALTER TABLE interview_sessions
    ADD COLUMN ai_model VARCHAR(80) NULL AFTER overall_score,
    ADD COLUMN prompt_version VARCHAR(20) NULL AFTER ai_model;
