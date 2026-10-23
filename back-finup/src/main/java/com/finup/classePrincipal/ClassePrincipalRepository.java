package com.finup.classePrincipal;

import com.finup.cnpjs.Cnpj;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassePrincipalRepository extends JpaRepository<ClassePrincipal, Long> {

    Optional<ClassePrincipal> findByNome(String nome);

    /**
     * Primeira classe com o nome informado (menor id).
     * Tolera nomes duplicados no banco, ao contrário de findByNome.
     */
    Optional<ClassePrincipal> findFirstByNomeOrderByIdAsc(String nome);
}
