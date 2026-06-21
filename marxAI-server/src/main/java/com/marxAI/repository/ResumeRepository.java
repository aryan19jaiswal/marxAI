package com.marxAI.repository;

import com.marxAI.model.entity.Resume;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Resume} records. */
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    /** Returns a user's uploaded resumes newest-first; the first entry is the active resume. */
    List<Resume> findByUserIdOrderByUploadedAtDesc(UUID userId);
}
