package com.finup.credencial;

import jdk.jfr.Registered;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredencialRepository {

    Optional<Credencial> findByUsername(String username);
}
