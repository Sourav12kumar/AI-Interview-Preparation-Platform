package com.sourav.interviewprep.resume.storage;

import com.sourav.interviewprep.resume.config.ResumeProperties;
import com.sourav.interviewprep.resume.exception.ResumeStorageException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class LocalResumeFileStorage implements ResumeFileStorage {

    private final Path storageRoot;

    public LocalResumeFileStorage(ResumeProperties properties) {
        this.storageRoot = properties.storageDir().toAbsolutePath().normalize();
    }

    @Override
    public String store(Long userId, String extension, byte[] content) {
        String storageKey = userId + "/" + UUID.randomUUID() + "." + extension;
        Path destination = resolve(storageKey);
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return storageKey;
        } catch (IOException exception) {
            throw new ResumeStorageException("Resume file could not be stored", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new ResumeStorageException("Resume file could not be deleted", exception);
        }
    }

    private Path resolve(String storageKey) {
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new ResumeStorageException("Invalid resume storage key", null);
        }
        return resolved;
    }
}
