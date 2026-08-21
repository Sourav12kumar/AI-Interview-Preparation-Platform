package com.sourav.interviewprep.resume.extraction;

public interface ResumeTextExtractor {
    ExtractedResume extract(String filename, byte[] content);
}
