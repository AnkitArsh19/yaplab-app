package com.arsh.project.secure_messaging.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
    Optional<User> findByEmailId(String emailId); //Method name matches the field
}
