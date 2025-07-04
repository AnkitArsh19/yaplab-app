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
     * Finds email change tokens for a user
     * @param user The user
     * @param tokenType The token type (EMAIL_CHANGE)
     * @return List of email change tokens for the user
     */
    List<EmailVerificationToken> findByUserAndTokenType(User user, EmailVerificationToken.TokenType tokenType);

    /**
     * Deletes all tokens for a specific user using a custom query
     * Modifying annotation is used to indicate that this is a modifying query
     * clearAutomatically is set to true to clear the persistence context after the operation
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM email_verification_token WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Deletes all tokens of a specific type for a user
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM email_verification_token WHERE user_id = :userId AND token_type = :tokenType", nativeQuery = true)
    void deleteByUserIdAndTokenType(@Param("userId") Long userId, @Param("tokenType") String tokenType);
}