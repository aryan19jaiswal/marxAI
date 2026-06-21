package com.marxAI.repository;

import com.marxAI.model.entity.StudyPlan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link StudyPlan} records. */
public interface StudyPlanRepository extends JpaRepository<StudyPlan, UUID> {

    /** Returns a user's study plans newest-first; the first entry is the current plan. */
    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
