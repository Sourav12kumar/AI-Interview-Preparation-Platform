package com.sourav.interviewprep.resume.service;

import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.interview.exception.AiGenerationException;
import com.sourav.interviewprep.profile.repository.CandidateProfileRepository;
import com.sourav.interviewprep.resume.ai.ResumeAnalysisContext;
import com.sourav.interviewprep.resume.ai.ResumeAnalysisResult;
import com.sourav.interviewprep.resume.ai.ResumeAnalyzer;
import com.sourav.interviewprep.resume.config.ResumeProperties;
import com.sourav.interviewprep.resume.dto.AnalyzeResumeRequest;
import com.sourav.interviewprep.resume.dto.ResumeResponse;
import com.sourav.interviewprep.resume.entity.ResumeEntity;
import com.sourav.interviewprep.resume.exception.ResumeNotFoundException;
import com.sourav.interviewprep.resume.exception.ResumeValidationException;
import com.sourav.interviewprep.resume.extraction.ExtractedResume;
import com.sourav.interviewprep.resume.extraction.ResumeTextExtractor;
import com.sourav.interviewprep.resume.repository.ResumeRepository;
import com.sourav.interviewprep.resume.storage.ResumeFileStorage;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ResumeService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeTextExtractor textExtractor;
    private final ResumeFileStorage fileStorage;
    private final ResumeAnalyzer resumeAnalyzer;
    private final ResumeProperties properties;
    private final ObjectMapper objectMapper;

    public ResumeService(
            UserRepository userRepository,
            CandidateProfileRepository profileRepository,
            ResumeRepository resumeRepository,
            ResumeTextExtractor textExtractor,
            ResumeFileStorage fileStorage,
            ResumeAnalyzer resumeAnalyzer,
            ResumeProperties properties,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.resumeRepository = resumeRepository;
        this.textExtractor = textExtractor;
        this.fileStorage = fileStorage;
        this.resumeAnalyzer = resumeAnalyzer;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public ResumeResponse upload(String email, MultipartFile file) {
        UserEntity user = currentUser(email);
        String filename = safeFilename(file.getOriginalFilename());
        byte[] content = readAndValidate(file);
        ExtractedResume extracted = textExtractor.extract(filename, content);
        String storageKey = fileStorage.store(user.getId(), extension(filename), content);
        try {
            ResumeEntity resume = resumeRepository.saveAndFlush(new ResumeEntity(
                    user, filename, storageKey, extracted.contentType(), content.length, extracted.text()));
            return response(resume);
        } catch (RuntimeException exception) {
            fileStorage.delete(storageKey);
            throw exception;
        }
    }

    public ResumeResponse analyze(String email, Long resumeId, AnalyzeResumeRequest request) {
        UserEntity user = currentUser(email);
        ResumeEntity resume = ownedResume(resumeId, user.getId());
        String targetRole = firstNonBlank(
                request.targetRole(),
                profileRepository.findByUser_Id(user.getId()).map(profile -> profile.getTargetRole()).orElse(null));
        if (targetRole == null) {
            throw new ResumeValidationException(
                    "Set a target role in the request or candidate profile before analysis");
        }

        resume.markProcessing(targetRole);
        resumeRepository.saveAndFlush(resume);
        try {
            ResumeAnalysisResult result = resumeAnalyzer.analyze(new ResumeAnalysisContext(
                    targetRole,
                    blankToNull(request.jobDescription()),
                    resume.getExtractedText()));
            resume.completeAnalysis(
                    result.atsScore(),
                    writeList(result.strengths()),
                    writeList(result.weaknesses()),
                    writeList(result.missingKeywords()),
                    result.summary(),
                    writeList(result.suggestions()),
                    result.model(),
                    result.promptVersion());
            return response(resumeRepository.saveAndFlush(resume));
        } catch (AiGenerationException exception) {
            resume.failAnalysis(exception.getMessage());
            resumeRepository.saveAndFlush(resume);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> list(String email) {
        UserEntity user = currentUser(email);
        return resumeRepository.findAllByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse get(String email, Long resumeId) {
        UserEntity user = currentUser(email);
        return response(ownedResume(resumeId, user.getId()));
    }

    public void delete(String email, Long resumeId) {
        UserEntity user = currentUser(email);
        ResumeEntity resume = ownedResume(resumeId, user.getId());
        fileStorage.delete(resume.getStorageKey());
        resumeRepository.delete(resume);
        resumeRepository.flush();
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeValidationException("Resume file is required");
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new ResumeValidationException("Resume file must not exceed 5 MB");
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResumeValidationException("Resume file could not be read", exception);
        }
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResumeValidationException("Resume filename is required");
        }
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (filename.isBlank() || filename.length() > 255 || filename.equals(".") || filename.equals("..")) {
            throw new ResumeValidationException("Resume filename is invalid");
        }
        return filename;
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private ResumeEntity ownedResume(Long resumeId, Long userId) {
        return resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(ResumeNotFoundException::new);
    }

    private ResumeResponse response(ResumeEntity resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getContentType(),
                resume.getFileSizeBytes(),
                resume.getAnalysisStatus(),
                resume.getTargetRole(),
                resume.getAtsScore(),
                resume.getAnalysisSummary(),
                readList(resume.getStrengths()),
                readList(resume.getWeaknesses()),
                readList(resume.getMissingKeywords()),
                readList(resume.getSuggestions()),
                resume.getAiModel(),
                resume.getPromptVersion(),
                resume.getFailureReason(),
                resume.getAnalyzedAt(),
                resume.getCreatedAt());
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            throw new IllegalStateException("Resume analysis could not be stored", exception);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> values = new ArrayList<>();
            root.forEach(node -> values.add(node.asText()));
            return List.copyOf(values);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored resume analysis is invalid", exception);
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) return preferred.trim();
        if (fallback != null && !fallback.isBlank()) return fallback.trim();
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserEntity currentUser(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user no longer exists"));
        if (!user.isActive()) {
            throw new UsernameNotFoundException("Authenticated user is not active");
        }
        return user;
    }
}
