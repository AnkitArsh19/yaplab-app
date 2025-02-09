package com.arsh.project.secure_messaging.Group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface to manage Groups entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface GroupRepository extends JpaRepository<Groups, Long>{
}
