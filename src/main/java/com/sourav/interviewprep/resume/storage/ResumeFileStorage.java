package com.sourav.interviewprep.resume.storage;

public interface ResumeFileStorage {
    String store(Long userId, String extension, byte[] content);
    void delete(String storageKey);
}
