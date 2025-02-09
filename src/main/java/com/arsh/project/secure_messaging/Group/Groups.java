package com.arsh.project.secure_messaging.Group;
import com.arsh.project.secure_messaging.User.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Group entity to store group_id, name, created_by, set of users, etc.
 */
@Entity
@Table(name = "group_table")
public class Groups {

    /**
     * Unique identifier for each group which is assigned automatically.
     * Long is preferred for large datasets.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * The name of the group (cannot be null).
     */
    @Column(name = "Group_name", nullable = false)
    private String name;

    /**
     * The creator of the group (cannot be null).
     */
    @ManyToOne
    @JoinColumn(name = "createdBy", nullable = false)
    private User createdBy;

    /**
     * A group can have many users and a user can be in many groups.
     * CascadeType is used to prevent unintended deletions and ensures changes propagate.
     * FetchType.Lazy ensures users are loaded only when required.
     * Hashset ensures unique users in the group.
     */
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_users",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();

    /**
     * Timestamp of when group was created.
     */
    @Column(name = "createdAt", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    public Groups() {
    }

    /**
     * Parameterized constructor.
     */
    public Groups(Integer id, String name, User createdBy, Set<User> users, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.users = users;
        this.createdAt = createdAt;
    }

    /**
     * Getters and setters.
     */
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}