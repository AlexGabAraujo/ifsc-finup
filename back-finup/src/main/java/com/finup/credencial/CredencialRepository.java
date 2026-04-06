package com.finup.credencial;

import com.finup.pessoaFisica.PessoaFisica;
import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredencialRepository extends JpaRepository<Credencial, Long> {

    Optional<Credencial> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Credencial findByPessoaFisicaId(Long id);

    @Query("SELECT c.pessoaFisica FROM Credencial c WHERE c.username = :username")
    Optional<PessoaFisica> findPessoaFisicaByUsername(String username);
}
