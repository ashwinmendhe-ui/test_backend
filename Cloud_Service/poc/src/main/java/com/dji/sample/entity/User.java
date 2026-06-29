package com.dji.sample.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    // FE compatibility only; later we can replace with deleted_at/status logic
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "company_id")
    private UUID companyId;

    // FE compatibility only; production does not need this
    // @Column(name = "company_name")
    // private String companyName;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_sites",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "site_id", referencedColumnName = "site_id")
    )
    private Set<Site> sites = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_missions",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "mission_id", referencedColumnName = "mission_id")
    )
    private Set<Mission> missions = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_devices",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "device_id", referencedColumnName = "device_id")
    )
    private Set<Device> devices = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (userId == null) {
            userId = UUID.randomUUID();
        }

        if (isActive == null) {
            isActive = true;
        }
    }
}