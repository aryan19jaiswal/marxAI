package com.marxAI.repository;

import com.marxAI.model.entity.Session;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Session} records. */
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /** Returns a user's sessions newest-first, used for session history sidebars. */
    List<Session> findByUserIdOrderByStartedAtDesc(UUID userId);
}
