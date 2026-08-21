package com.sourav.interviewprep.resume.controller;

import com.sourav.interviewprep.resume.dto.AnalyzeResumeRequest;
import com.sourav.interviewprep.resume.dto.ResumeResponse;
import com.sourav.interviewprep.resume.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ResumeResponse> upload(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.upload(authentication.getName(), file));
    }

    @PostMapping("/{resumeId}/analysis")
    ResumeResponse analyze(
            Authentication authentication,
            @PathVariable Long resumeId,
            @Valid @RequestBody AnalyzeResumeRequest request) {
        return resumeService.analyze(authentication.getName(), resumeId, request);
    }

    @GetMapping
    List<ResumeResponse> list(Authentication authentication) {
        return resumeService.list(authentication.getName());
    }

    @GetMapping("/{resumeId}")
    ResumeResponse get(Authentication authentication, @PathVariable Long resumeId) {
        return resumeService.get(authentication.getName(), resumeId);
    }

    @DeleteMapping("/{resumeId}")
    ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long resumeId) {
        resumeService.delete(authentication.getName(), resumeId);
        return ResponseEntity.noContent().build();
    }
}
