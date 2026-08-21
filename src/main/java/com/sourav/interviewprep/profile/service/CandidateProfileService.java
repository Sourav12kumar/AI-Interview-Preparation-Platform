package com.sourav.interviewprep.profile.service;

import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.profile.dto.CandidateProfileResponse;
import com.sourav.interviewprep.profile.dto.ProfileUpsertRequest;
import com.sourav.interviewprep.profile.dto.SkillCreateRequest;
import com.sourav.interviewprep.profile.dto.SkillResponse;
import com.sourav.interviewprep.profile.dto.SkillUpdateRequest;
import com.sourav.interviewprep.profile.entity.CandidateProfileEntity;
import com.sourav.interviewprep.profile.entity.SkillEntity;
import com.sourav.interviewprep.profile.entity.TargetCompanyEntity;
import com.sourav.interviewprep.profile.entity.UserSkillEntity;
import com.sourav.interviewprep.profile.exception.DuplicateSkillAssignmentException;
import com.sourav.interviewprep.profile.exception.ProfileNotFoundException;
import com.sourav.interviewprep.profile.exception.SkillAssignmentNotFoundException;
import com.sourav.interviewprep.profile.repository.CandidateProfileRepository;
import com.sourav.interviewprep.profile.repository.SkillRepository;
import com.sourav.interviewprep.profile.repository.TargetCompanyRepository;
import com.sourav.interviewprep.profile.repository.UserSkillRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CandidateProfileService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final TargetCompanyRepository targetCompanyRepository;

    public CandidateProfileService(
            UserRepository userRepository,
            CandidateProfileRepository profileRepository,
            SkillRepository skillRepository,
            UserSkillRepository userSkillRepository,
            TargetCompanyRepository targetCompanyRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.targetCompanyRepository = targetCompanyRepository;
    }

    @Transactional(readOnly = true)
    public CandidateProfileResponse get(String email) {
        UserEntity user = currentUser(email);
        CandidateProfileEntity profile = profileRepository.findByUser_Id(user.getId())
                .orElseThrow(ProfileNotFoundException::new);
        return response(profile, user.getId());
    }

    @Transactional
    public CandidateProfileResponse upsert(String email, ProfileUpsertRequest request) {
        UserEntity user = currentUser(email);
        CandidateProfileEntity profile = profileRepository.findByUser_Id(user.getId())
                .orElseGet(() -> new CandidateProfileEntity(user));
        profile.update(
                request.headline(),
                request.phone(),
                request.location(),
                request.educationLevel(),
                request.institution(),
                request.graduationYear(),
                request.yearsOfExperience(),
                request.targetRole(),
                request.bio());
        profileRepository.saveAndFlush(profile);
        synchronizeTargetCompanies(user, request.targetCompanies());
        return response(profile, user.getId());
    }

    @Transactional
    public void delete(String email) {
        UserEntity user = currentUser(email);
        CandidateProfileEntity profile = profileRepository.findByUser_Id(user.getId())
                .orElseThrow(ProfileNotFoundException::new);
        userSkillRepository.deleteByUser_Id(user.getId());
        targetCompanyRepository.deleteByUser_Id(user.getId());
        profileRepository.delete(profile);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkills(String email) {
        UserEntity user = currentUser(email);
        return skillResponses(user.getId());
    }

    @Transactional
    public SkillResponse addSkill(String email, SkillCreateRequest request) {
        UserEntity user = currentUser(email);
        SkillEntity skill = skillRepository.findByNameIgnoreCase(request.name().trim())
                .orElseGet(() -> skillRepository.saveAndFlush(
                        new SkillEntity(request.name(), request.category())));
        if (userSkillRepository.findByUser_IdAndSkill_Id(user.getId(), skill.getId()).isPresent()) {
            throw new DuplicateSkillAssignmentException();
        }
        UserSkillEntity assignment = new UserSkillEntity(
                user, skill, request.proficiency(), request.yearsUsed());
        return SkillResponse.from(userSkillRepository.save(assignment));
    }

    @Transactional
    public SkillResponse updateSkill(String email, Long skillId, SkillUpdateRequest request) {
        UserEntity user = currentUser(email);
        UserSkillEntity assignment = ownedSkill(user.getId(), skillId);
        assignment.update(request.proficiency(), request.yearsUsed());
        return SkillResponse.from(assignment);
    }

    @Transactional
    public void removeSkill(String email, Long skillId) {
        UserEntity user = currentUser(email);
        userSkillRepository.delete(ownedSkill(user.getId(), skillId));
    }

    private UserSkillEntity ownedSkill(Long userId, Long skillId) {
        return userSkillRepository.findByUser_IdAndSkill_Id(userId, skillId)
                .orElseThrow(SkillAssignmentNotFoundException::new);
    }

    private CandidateProfileResponse response(CandidateProfileEntity profile, Long userId) {
        List<String> companies = targetCompanyRepository.findAllByUser_IdOrderByCompanyNameAsc(userId)
                .stream()
                .map(TargetCompanyEntity::getCompanyName)
                .toList();
        return CandidateProfileResponse.from(profile, companies, skillResponses(userId));
    }

    private List<SkillResponse> skillResponses(Long userId) {
        return userSkillRepository.findAllByUser_IdOrderBySkill_NameAsc(userId)
                .stream()
                .map(SkillResponse::from)
                .toList();
    }

    private void synchronizeTargetCompanies(UserEntity user, List<String> requestedCompanies) {
        Map<String, String> requested = new LinkedHashMap<>();
        if (requestedCompanies != null) {
            requestedCompanies.forEach(name -> requested.putIfAbsent(normalize(name), name.trim()));
        }

        List<TargetCompanyEntity> existing = targetCompanyRepository
                .findAllByUser_IdOrderByCompanyNameAsc(user.getId());
        Map<String, TargetCompanyEntity> existingByName = new LinkedHashMap<>();
        existing.forEach(company -> existingByName.put(normalize(company.getCompanyName()), company));

        List<TargetCompanyEntity> removed = existing.stream()
                .filter(company -> !requested.containsKey(normalize(company.getCompanyName())))
                .toList();
        targetCompanyRepository.deleteAll(removed);

        List<TargetCompanyEntity> saved = new ArrayList<>();
        requested.forEach((normalizedName, displayName) -> {
            TargetCompanyEntity company = existingByName.get(normalizedName);
            if (company == null) {
                company = new TargetCompanyEntity(user, displayName);
            } else {
                company.rename(displayName);
            }
            saved.add(company);
        });
        targetCompanyRepository.saveAll(saved);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
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
