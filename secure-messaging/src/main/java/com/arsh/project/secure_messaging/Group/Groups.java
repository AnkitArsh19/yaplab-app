package com.arsh.project.secure_messaging.Group;
import com.arsh.project.secure_messaging.User.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Group")
public class Groups {

    //Generates unique id for each group created to store in the database
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    //Stores the required group name for each group
    @Column(name = "Group_name", nullable = false)
    private String name;

    //Stores the details of the user who created the group
    @Column(name = "createdBy")
    private User createdBy;

    //Many_to_many relationship. Stores the list of users in the group.
    @ManyToMany
    @JoinTable(
            name = "group_users",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();


    // Timestamp when the group was created
    @Column(name = "createdAt", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    //Default constructor
    public Groups() {
    }

    //Parameterized constructor
    public Groups(Integer id, String name, User createdBy, Set<User> users, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.users = users;
        this.createdAt = createdAt;
    }

    //Getters and setters
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