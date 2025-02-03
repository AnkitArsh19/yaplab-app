package com.arsh.project.secure_messaging.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
//Repository interface for database interactions
public interface UserRepo extends JpaRepository<User, Integer> {
    //findBy method defined to use in the service layer. Will automatically generate SQL query.
    Optional<User> findByEmailId(String emailId); //Method name matches the field
}
