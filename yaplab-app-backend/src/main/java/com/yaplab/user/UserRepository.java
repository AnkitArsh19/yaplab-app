package com.yaplab.user;

import com.yaplab.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface to manage User entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Retrieves user details from the email id provided.
     * @param emailId The emailId of the user
     */
    Optional<User> findByEmailId(String emailId);

    /**
     * Retrieves user details from the mobile number provided.
     * @param mobileNumber The mobile number of the user
     */
    Optional<User> findByMobileNumber(String mobileNumber);

    /**
     * Returns a list of users with the same status(connected or disconnected)
     * @param status The current status of the user.
     * @return List of user entity.
     */
    List<User> findByStatus(UserStatus status);

    /**
     * Return a list of users with the searched set of characters with priority ordering.
     * Priority: 1. Name matches (starting with query gets highest priority)
     *          2. Email matches
     *          3. Mobile number matches
     * @param query The search query
     * @return List of user entity ordered by relevance
     */
    @Query("""
        SELECT u,
        CASE
            WHEN LOWER(u.userName) LIKE LOWER(CONCAT(:query, '%')) THEN 1
            WHEN LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%')) THEN 2
            WHEN LOWER(u.emailId) LIKE LOWER(CONCAT('%', :query, '%')) THEN 3
            WHEN u.mobileNumber LIKE CONCAT('%', :query, '%') THEN 4
            ELSE 5
        END as priority
        FROM User u
        WHERE (LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(u.emailId) LIKE LOWER(CONCAT('%', :query, '%'))
           OR u.mobileNumber LIKE CONCAT('%', :query, '%'))
        ORDER BY
        CASE
            WHEN LOWER(u.userName) LIKE LOWER(CONCAT(:query, '%')) THEN 1
            WHEN LOWER(u.userName) LIKE LOWER(CONCAT('%', :query, '%')) THEN 2
            WHEN LOWER(u.emailId) LIKE LOWER(CONCAT('%', :query, '%')) THEN 3
            WHEN u.mobileNumber LIKE CONCAT('%', :query, '%') THEN 4
            ELSE 5
        END ASC,
        LOWER(u.userName) ASC
        """)
    List<User> findUsersWithPriority(@Param("query") String query);
}