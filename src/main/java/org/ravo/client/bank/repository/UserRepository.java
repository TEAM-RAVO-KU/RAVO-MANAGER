package org.ravo.client.bank.repository;

import org.ravo.client.bank.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
