package com.sourav.interviewprep.admin.controller;

import com.sourav.interviewprep.admin.dto.AdminCodingProblemRequest;
import com.sourav.interviewprep.admin.dto.AdminCodingProblemResponse;
import com.sourav.interviewprep.admin.dto.AdminOverviewResponse;
import com.sourav.interviewprep.admin.dto.AdminUserResponse;
import com.sourav.interviewprep.admin.dto.AdminUserStatusRequest;
import com.sourav.interviewprep.admin.dto.AuditEventResponse;
import com.sourav.interviewprep.admin.service.AdminService;
import com.sourav.interviewprep.auth.entity.AccountStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    AdminOverviewResponse overview() {
        return adminService.overview();
    }

    @GetMapping("/users")
    List<AdminUserResponse> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "100") int limit) {
        return adminService.listUsers(q, status, limit);
    }

    @PatchMapping("/users/{userId}/status")
    AdminUserResponse changeUserStatus(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusRequest request) {
        return adminService.changeUserStatus(authentication.getName(), userId, request);
    }

    @GetMapping("/coding/problems")
    List<AdminCodingProblemResponse> listProblems() {
        return adminService.listProblems();
    }

    @PostMapping("/coding/problems")
    ResponseEntity<AdminCodingProblemResponse> createProblem(
            Authentication authentication,
            @Valid @RequestBody AdminCodingProblemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createProblem(authentication.getName(), request));
    }

    @PutMapping("/coding/problems/{problemId}")
    AdminCodingProblemResponse updateProblem(
            Authentication authentication,
            @PathVariable Long problemId,
            @Valid @RequestBody AdminCodingProblemRequest request) {
        return adminService.updateProblem(authentication.getName(), problemId, request);
    }

    @DeleteMapping("/coding/problems/{problemId}")
    ResponseEntity<Void> deactivateProblem(
            Authentication authentication,
            @PathVariable Long problemId) {
        adminService.deactivateProblem(authentication.getName(), problemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-events")
    List<AuditEventResponse> listAuditEvents(
            @RequestParam(defaultValue = "100") int limit) {
        return adminService.listAuditEvents(limit);
    }
}
