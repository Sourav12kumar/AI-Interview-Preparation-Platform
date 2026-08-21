package com.sourav.interviewprep.coding.controller;

import com.sourav.interviewprep.coding.dto.CodingProblemResponse;
import com.sourav.interviewprep.coding.dto.CodingProblemSummaryResponse;
import com.sourav.interviewprep.coding.dto.CodingSubmissionResponse;
import com.sourav.interviewprep.coding.dto.CodingSubmissionSummaryResponse;
import com.sourav.interviewprep.coding.dto.SubmitCodeRequest;
import com.sourav.interviewprep.coding.entity.CodingDifficulty;
import com.sourav.interviewprep.coding.service.CodingPracticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coding")
public class CodingPracticeController {

    private final CodingPracticeService codingPracticeService;

    public CodingPracticeController(CodingPracticeService codingPracticeService) {
        this.codingPracticeService = codingPracticeService;
    }

    @GetMapping("/problems")
    List<CodingProblemSummaryResponse> listProblems(
            @RequestParam(required = false) CodingDifficulty difficulty,
            @RequestParam(required = false) String tag) {
        return codingPracticeService.listProblems(difficulty, tag);
    }

    @GetMapping("/problems/{problemId}")
    CodingProblemResponse getProblem(@PathVariable Long problemId) {
        return codingPracticeService.getProblem(problemId);
    }

    @PostMapping("/problems/{problemId}/submissions")
    ResponseEntity<CodingSubmissionResponse> submit(
            Authentication authentication,
            @PathVariable Long problemId,
            @Valid @RequestBody SubmitCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(codingPracticeService.submit(authentication.getName(), problemId, request));
    }

    @GetMapping("/submissions")
    List<CodingSubmissionSummaryResponse> listSubmissions(Authentication authentication) {
        return codingPracticeService.listSubmissions(authentication.getName());
    }

    @GetMapping("/submissions/{submissionId}")
    CodingSubmissionResponse getSubmission(
            Authentication authentication,
            @PathVariable Long submissionId) {
        return codingPracticeService.getSubmission(authentication.getName(), submissionId);
    }
}
