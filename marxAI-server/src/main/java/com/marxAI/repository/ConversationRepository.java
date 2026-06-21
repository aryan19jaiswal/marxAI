package com.marxAI.repository;

import com.marxAI.model.entity.Conversation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Conversation} turns within a session. */
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Returns a session's messages oldest-first, used to reconstruct chat history/memory. */
    List<Conversation> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
