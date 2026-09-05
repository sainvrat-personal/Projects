package com.harGharAaurved.repository;
import com.harGharAaurved.Entity.user;
import java.util.Optional;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<user,Long>{
    Optional<user> findByEmail(String email);
    Optional<user> findByUsernameOrEmail(String username, String email);
    Optional<user> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

}
