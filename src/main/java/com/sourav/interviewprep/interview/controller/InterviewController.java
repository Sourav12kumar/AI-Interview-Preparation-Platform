package com.sourav.interviewprep.interview.controller;

import com.sourav.interviewprep.interview.dto.CreateInterviewRequest;
import com.sourav.interviewprep.interview.dto.InterviewSessionResponse;
import com.sourav.interviewprep.interview.dto.InterviewSessionSummaryResponse;
import com.sourav.interviewprep.interview.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    ResponseEntity<InterviewSessionResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateInterviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.create(authentication.getName(), request));
    }

    @GetMapping
    List<InterviewSessionSummaryResponse> list(Authentication authentication) {
        return interviewService.list(authentication.getName());
    }

    @GetMapping("/{sessionId}")
    InterviewSessionResponse get(Authentication authentication, @PathVariable Long sessionId) {
        return interviewService.get(authentication.getName(), sessionId);
    }
}
