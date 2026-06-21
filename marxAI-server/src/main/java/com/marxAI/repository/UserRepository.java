package com.marxAI.repository;

import com.marxAI.model.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link User} records, keyed by login email. */
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Looks up a user by their unique email, used during login/registration checks. */
    Optional<User> findByEmail(String email);

    /** Cheap existence check for registration email-uniqueness validation. */
    boolean existsByEmail(String email);
}
