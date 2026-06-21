package com.marxAI.repository;

import com.marxAI.model.entity.Document;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Document} records. */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /** Returns a user's uploaded documents newest-first, used by the documents management UI. */
    List<Document> findByUserIdOrderByUploadedAtDesc(UUID userId);
}
