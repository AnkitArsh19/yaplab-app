package com.ankitarsh.securemessaging.User;

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
    Optional<User> findByEmailId(String emailId); //Method userName matches the field
    List<User> findByStatus(UserStatus status);
}
