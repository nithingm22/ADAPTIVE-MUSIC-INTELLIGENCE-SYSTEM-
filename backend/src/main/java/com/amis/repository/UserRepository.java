package com.amis.repository;

import com.amis.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * UserRepository - Database operations for User entity.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Find a user by their email (used for login)
    Optional<User> findByEmail(String email);

    // Check if an email is already registered
    boolean existsByEmail(String email);
}
