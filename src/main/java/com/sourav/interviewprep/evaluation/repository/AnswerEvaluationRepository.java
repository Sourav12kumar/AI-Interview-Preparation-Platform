package com.sourav.interviewprep.evaluation.repository;

import com.sourav.interviewprep.evaluation.entity.AnswerEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluationEntity, Long> {
    Optional<AnswerEvaluationEntity> findByAnswer_Id(Long answerId);

    @Query("select evaluation from AnswerEvaluationEntity evaluation "
            + "where evaluation.answer.question.session.id = :sessionId")
    List<AnswerEvaluationEntity> findAllBySessionId(@Param("sessionId") Long sessionId);

    @Query("select evaluation from AnswerEvaluationEntity evaluation "
            + "join fetch evaluation.answer answer "
            + "join fetch answer.question question "
            + "join fetch question.session session "
            + "where session.user.id = :userId "
            + "and evaluation.evaluatedAt >= :from and evaluation.evaluatedAt < :to")
    List<AnswerEvaluationEntity> findAllInPeriod(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
