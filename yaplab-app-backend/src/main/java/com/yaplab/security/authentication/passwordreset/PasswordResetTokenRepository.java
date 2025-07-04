package com.yaplab.security.authentication.passwordreset;

import com.yaplab.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface to manage Password reset token entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a PasswordResetToken by its token value.
     */
    Optional<PasswordResetToken> findByToken(String Token);

    /**
     * Deletes a PasswordResetToken by its user.
     * @param user The user associated with the token to be deleted.
     */
    void deleteByUser(User user);
}
