package com.sourav.interviewprep.common.exception;

import com.sourav.interviewprep.auth.exception.DuplicateEmailException;
import com.sourav.interviewprep.auth.exception.InvalidTokenException;
import com.sourav.interviewprep.coding.exception.CodeExecutionException;
import com.sourav.interviewprep.coding.exception.CodeRunnerUnavailableException;
import com.sourav.interviewprep.coding.exception.CodingProblemNotFoundException;
import com.sourav.interviewprep.coding.exception.CodingSubmissionNotFoundException;
import com.sourav.interviewprep.evaluation.exception.DuplicateAnswerException;
import com.sourav.interviewprep.interview.exception.AiGenerationException;
import com.sourav.interviewprep.interview.exception.InterviewConfigurationException;
import com.sourav.interviewprep.interview.exception.InterviewNotFoundException;
import com.sourav.interviewprep.profile.exception.DuplicateSkillAssignmentException;
import com.sourav.interviewprep.profile.exception.ProfileNotFoundException;
import com.sourav.interviewprep.profile.exception.SkillAssignmentNotFoundException;
import com.sourav.interviewprep.resume.exception.ResumeNotFoundException;
import com.sourav.interviewprep.resume.exception.ResumeStorageException;
import com.sourav.interviewprep.resume.exception.ResumeValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateSkillAssignmentException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateSkill(DuplicateSkillAssignmentException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateAnswerException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateAnswer(DuplicateAnswerException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({ProfileNotFoundException.class, SkillAssignmentNotFoundException.class})
    ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InterviewNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleInterviewNotFound(InterviewNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ResumeNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleResumeNotFound(ResumeNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({CodingProblemNotFoundException.class, CodingSubmissionNotFoundException.class})
    ResponseEntity<ApiErrorResponse> handleCodingNotFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InterviewConfigurationException.class)
    ResponseEntity<ApiErrorResponse> handleInterviewConfiguration(InterviewConfigurationException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ResumeValidationException.class)
    ResponseEntity<ApiErrorResponse> handleResumeValidation(ResumeValidationException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "Resume file must not exceed 5 MB", Map.of());
    }

    @ExceptionHandler(ResumeStorageException.class)
    ResponseEntity<ApiErrorResponse> handleResumeStorage(ResumeStorageException exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Resume storage operation failed", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST, "Request body is malformed or contains an invalid value", Map.of());
    }

    @ExceptionHandler(AiGenerationException.class)
    ResponseEntity<ApiErrorResponse> handleAiGeneration(AiGenerationException exception) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CodeRunnerUnavailableException.class)
    ResponseEntity<ApiErrorResponse> handleCodeRunnerUnavailable(CodeRunnerUnavailableException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CodeExecutionException.class)
    ResponseEntity<ApiErrorResponse> handleCodeExecution(CodeExecutionException exception) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({InvalidTokenException.class, AuthenticationException.class})
    ResponseEntity<ApiErrorResponse> handleUnauthorized(RuntimeException exception) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), Map.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String message,
            Map<String, String> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
