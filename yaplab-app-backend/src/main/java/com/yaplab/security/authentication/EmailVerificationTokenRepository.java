package com.yaplab.security.authentication;

import com.yaplab.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Finds an email verification token by token.
     * @param token email verification token
     * @return An email verification token if found
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Finds a list of all email verification tokens for a user.
     * @param user The user for whom the tokens are to be found
     * @return A list of email verification tokens for the user
     */
    List<EmailVerificationToken> findByUser(User user);

    /**
     * Deletes all tokens for a specific user using a custom query
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM email_verification_token WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);
}