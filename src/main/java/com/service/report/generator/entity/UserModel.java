package com.service.report.generator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Comment("The user table stores essential information about individuals accessing the application, providing a centralized repository for managing user data within the system.")
@Table(
        name = "user",
        indexes = @Index(name = "idx_use_uid", columnList = "use_uid")
)
public class UserModel extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Uniquely identifies each user within the database.")
    @Column(name = "use_pid")
    private Long userPid;

    @Comment("This column is used to store the user id in string format")
    @Column(name = "use_uid", unique = true, nullable = false)
    private String userUid;

    @Comment("This column is used to store date and time of user token")
    @Column(name = "use_token_at")
    private Instant tokenAt;

    @Comment("This column stores email address of user.")
    @Column(name = "use_email", length = 100, nullable = false, unique = true)
    private String email;

    @Comment("This column stores password of user in encrypted form.")
    @Column(name = "use_password", nullable = false)
    private String password;

    @ManyToMany(
            fetch = FetchType.LAZY,
            cascade = {
                    CascadeType.MERGE,
                    CascadeType.REFRESH
            }
    )
    @JoinTable(
            name = "user_role_mapping",
            joinColumns = {
                    @JoinColumn(name = "use_id", nullable = false)
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "role_id", nullable = false)
            }
    )
    private List<UserRoleModel> userRoles;

}