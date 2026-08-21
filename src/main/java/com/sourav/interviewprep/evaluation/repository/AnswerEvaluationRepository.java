package com.sourav.interviewprep.evaluation.repository;

import com.sourav.interviewprep.evaluation.entity.AnswerEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluationEntity, Long> {
    Optional<AnswerEvaluationEntity> findByAnswer_Id(Long answerId);

    @Query("select evaluation from AnswerEvaluationEntity evaluation "
            + "where evaluation.answer.question.session.id = :sessionId")
    List<AnswerEvaluationEntity> findAllBySessionId(@Param("sessionId") Long sessionId);
}
