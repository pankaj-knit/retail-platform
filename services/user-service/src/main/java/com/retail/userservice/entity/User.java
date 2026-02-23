package com.retail.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA Entity mapped to the "users" table in Postgres.
 *
 * Lombok annotations used (safe subset for JPA entities):
 *   - @Getter:            Generates all getter methods
 *   - @Setter:            Generates all setter methods
 *   - @NoArgsConstructor: Generates the no-arg constructor that JPA requires
 *   - @AllArgsConstructor: Required by @Builder (Builder needs a way to set all fields)
 *   - @Builder:           Generates a fluent builder API: User.builder().email("...").build()
 *
 * Lombok annotations intentionally SKIPPED:
 *   - @Data:             Generates equals/hashCode using ALL fields. Dangerous
 *                        with JPA because the id is null before save() and
 *                        assigned after -- this breaks HashSets and Maps.
 *   - @ToString:         Can trigger lazy-loading of @OneToMany/@ManyToOne
 *                        relations, causing N+1 queries or
 *                        LazyInitializationException outside a transaction.
 *   - @EqualsAndHashCode: Same problem as @Data. For JPA entities, equals/hashCode
 *                        should use only the business key (email) or be left
 *                        as the default Object identity.
 */
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
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return id != null && id.equals(u.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
