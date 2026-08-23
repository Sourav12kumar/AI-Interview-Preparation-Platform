package com.sourav.interviewprep.profile.controller;

import com.sourav.interviewprep.profile.dto.CandidateProfileResponse;
import com.sourav.interviewprep.profile.dto.ProfileUpsertRequest;
import com.sourav.interviewprep.profile.dto.SkillCreateRequest;
import com.sourav.interviewprep.profile.dto.SkillResponse;
import com.sourav.interviewprep.profile.dto.SkillUpdateRequest;
import com.sourav.interviewprep.profile.service.CandidateProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile")
public class CandidateProfileController {

    private final CandidateProfileService profileService;

    public CandidateProfileController(CandidateProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    CandidateProfileResponse get(Authentication authentication) {
        return profileService.get(authentication.getName());
    }

    @PutMapping
    CandidateProfileResponse upsert(
            Authentication authentication,
            @Valid @RequestBody ProfileUpsertRequest request) {
        return profileService.upsert(authentication.getName(), request);
    }

    @DeleteMapping
    ResponseEntity<Void> delete(Authentication authentication) {
        profileService.delete(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/skills")
    List<SkillResponse> listSkills(Authentication authentication) {
        return profileService.listSkills(authentication.getName());
    }

    @PostMapping("/skills")
    ResponseEntity<SkillResponse> addSkill(
            Authentication authentication,
            @Valid @RequestBody SkillCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.addSkill(authentication.getName(), request));
    }

    @PutMapping("/skills/{skillId}")
    SkillResponse updateSkill(
            Authentication authentication,
            @PathVariable Long skillId,
            @Valid @RequestBody SkillUpdateRequest request) {
        return profileService.updateSkill(authentication.getName(), skillId, request);
    }

    @DeleteMapping("/skills/{skillId}")
    ResponseEntity<Void> removeSkill(Authentication authentication, @PathVariable Long skillId) {
        profileService.removeSkill(authentication.getName(), skillId);
        return ResponseEntity.noContent().build();
    }
}
