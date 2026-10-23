package com.finup.subclasse;

import com.finup.cnpjs.Cnpj;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubClasseRepository extends JpaRepository<SubClasse, Long> {

    List<SubClasse> findByClassePrincipalId(Long classePrincipalId);

    /**
     * Primeira subclasse com o nome informado dentro da classe principal (menor id).
     */
    Optional<SubClasse> findFirstByNomeAndClassePrincipalIdOrderByIdAsc(String nome, Long classePrincipalId);

}
