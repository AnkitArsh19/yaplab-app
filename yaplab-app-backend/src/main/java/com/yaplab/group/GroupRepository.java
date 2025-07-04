package com.yaplab.group;

import com.yaplab.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface to manage Groups entity.
 * Extends JPARepository to perform CRUD operations.
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long>{
    
    /**
     * Finds all groups created by a specific user
     * @param createdBy The user who created the groups
     * @return List of groups created by the user
     */
    List<Group> findByCreatedBy(User createdBy);
    
    /**
     * Removes a user from all groups' user lists (group_users table)
     * @param userId The ID of the user to remove
     */
    @Modifying
    @Query(value = "DELETE FROM group_users WHERE user_id = :userId", nativeQuery = true)
    void removeUserFromAllGroups(@Param("userId") Long userId);
    
    /**
     * Deletes all groups created by a specific user
     * @param userId The ID of the user who created the groups
     */
    @Modifying
    @Query("DELETE FROM Group g WHERE g.createdBy.id = :userId")
    void deleteGroupsByCreatedBy(@Param("userId") Long userId);
}