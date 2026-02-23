package com.retail.userservice.repository;

import com.retail.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for the User entity.
 *
 * By extending JpaRepository, we get all standard CRUD operations for free:
 *   - save(user)        -> INSERT or UPDATE
 *   - findById(id)      -> SELECT WHERE id = ?
 *   - findAll()         -> SELECT *
 *   - deleteById(id)    -> DELETE WHERE id = ?
 *   - count()           -> SELECT COUNT(*)
 *   ... and many more.
 *
 * Spring Data also generates query implementations from method names.
 * "findByEmail" is parsed as: SELECT * FROM users WHERE email = ?
 * No SQL or implementation code needed -- Spring generates it at startup.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
