package com.sourav.interviewprep.evaluation.repository;

import com.sourav.interviewprep.evaluation.entity.InterviewAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswerEntity, Long> {
    boolean existsByQuestion_Id(Long questionId);
    long countByQuestion_Session_Id(Long sessionId);
    List<InterviewAnswerEntity> findAllByQuestion_Session_IdOrderByQuestion_SequenceNumberAsc(Long sessionId);
}
