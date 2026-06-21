package com.marxAI.repository;

import com.marxAI.model.entity.Progress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Progress} records. */
public interface ProgressRepository extends JpaRepository<Progress, UUID> {

    /** Returns all topic/subtopic progress rows for a user, used to assess overall level. */
    List<Progress> findByUserId(UUID userId);

    /** Looks up the progress row for one specific topic/subtopic pair to update after a session. */
    Optional<Progress> findByUserIdAndTopicAndSubtopic(UUID userId, String topic, String subtopic);
}
