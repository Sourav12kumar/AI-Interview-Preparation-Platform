package com.sourav.interviewprep.interview.repository;

import com.sourav.interviewprep.interview.entity.InterviewQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestionEntity, Long> {
    List<InterviewQuestionEntity> findAllBySession_IdOrderBySequenceNumberAsc(Long sessionId);
    Optional<InterviewQuestionEntity> findByIdAndSession_Id(Long id, Long sessionId);
}
