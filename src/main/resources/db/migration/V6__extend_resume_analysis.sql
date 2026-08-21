ALTER TABLE resumes
    ADD COLUMN target_role VARCHAR(120) NULL AFTER extracted_text,
    ADD COLUMN analysis_summary TEXT NULL AFTER missing_keywords,
    ADD COLUMN suggestions JSON NULL AFTER analysis_summary,
    ADD COLUMN ai_model VARCHAR(80) NULL AFTER suggestions,
    ADD COLUMN prompt_version VARCHAR(30) NULL AFTER ai_model,
    ADD COLUMN failure_reason VARCHAR(500) NULL AFTER prompt_version;

ALTER TABLE resumes
    ADD CONSTRAINT chk_resumes_file_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 5242880),
    ADD CONSTRAINT chk_resumes_ats_score CHECK (ats_score IS NULL OR (ats_score >= 0 AND ats_score <= 100));
