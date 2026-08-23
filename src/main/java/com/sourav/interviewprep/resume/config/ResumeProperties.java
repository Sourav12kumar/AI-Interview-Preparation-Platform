package com.sourav.interviewprep.resume.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.resume")
public record ResumeProperties(
        Path storageDir,
        DataSize maxFileSize,
        int maxExtractedCharacters
) {
    private static final DataSize ABSOLUTE_MAX_FILE_SIZE = DataSize.ofMegabytes(5);

    public ResumeProperties {
        storageDir = storageDir == null ? Path.of("./data/resumes") : storageDir;
        maxFileSize = maxFileSize == null ? DataSize.ofMegabytes(5) : maxFileSize;
        if (maxFileSize.compareTo(ABSOLUTE_MAX_FILE_SIZE) > 0) {
            maxFileSize = ABSOLUTE_MAX_FILE_SIZE;
        }
        maxExtractedCharacters = maxExtractedCharacters <= 0 ? 100_000 : maxExtractedCharacters;
    }
}
