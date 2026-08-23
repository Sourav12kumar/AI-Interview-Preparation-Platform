package com.sourav.interviewprep.evaluation.controller;

import com.sourav.interviewprep.evaluation.dto.SubmitAnswerRequest;
import com.sourav.interviewprep.evaluation.dto.SubmittedAnswerResponse;
import com.sourav.interviewprep.evaluation.service.AnswerEvaluationService;
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
@RequestMapping("/api/v1/interviews/{sessionId}/answers")
public class AnswerEvaluationController {

    private final AnswerEvaluationService evaluationService;

    public AnswerEvaluationController(AnswerEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    ResponseEntity<SubmittedAnswerResponse> submit(
            Authentication authentication,
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evaluationService.submit(authentication.getName(), sessionId, request));
    }

    @GetMapping
    List<SubmittedAnswerResponse> list(Authentication authentication, @PathVariable Long sessionId) {
        return evaluationService.list(authentication.getName(), sessionId);
    }
}
