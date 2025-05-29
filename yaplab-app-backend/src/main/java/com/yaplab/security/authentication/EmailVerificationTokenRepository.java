package com.yaplab.security.authentication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface to manage Email verification token entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Finds an email verification token object from the unique token
     */
    Optional<EmailVerificationToken> findByToken(String token);
}