CREATE TABLE target_companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_target_companies_user_name UNIQUE (user_id, company_name),
    CONSTRAINT fk_target_companies_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_target_companies_user (user_id)
) ENGINE=InnoDB;

ALTER TABLE user_profiles
    ADD CONSTRAINT chk_profiles_years_experience
        CHECK (years_of_experience >= 0 AND years_of_experience <= 99.9);

ALTER TABLE user_profiles
    ADD CONSTRAINT chk_profiles_graduation_year
        CHECK (graduation_year IS NULL OR graduation_year BETWEEN 1900 AND 2200);

ALTER TABLE user_skills
    ADD CONSTRAINT chk_user_skills_years_used
        CHECK (years_used >= 0 AND years_used <= 99.9);
