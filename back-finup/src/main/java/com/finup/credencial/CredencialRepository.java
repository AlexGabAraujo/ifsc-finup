package com.finup.credencial;

import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredencialRepository extends JpaRepository<Credencial, Long> {

    Optional<Credencial> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
