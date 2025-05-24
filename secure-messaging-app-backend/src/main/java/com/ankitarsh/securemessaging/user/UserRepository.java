package com.ankitarsh.securemessaging.user;

import com.ankitarsh.securemessaging.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface to manage Groups entity.
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
     * Returns a list of users with the same status(connected or disconnected)
     * @param status The current status of the user.
     * @return List of user entity.
     */
    List<User> findByStatus(UserStatus status);

    /**
     * Return a list of users with the searched set of characters. Can be used to search a particular user.
     * @param userName The set of letters to search with.
     * @return List of user entity
     */
    List<User> findByUserNameContainingIgnoreCase(String userName);
}
