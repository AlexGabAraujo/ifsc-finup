package com.finup.cnaeapi.cnae;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CnaeRepository extends JpaRepository<Cnae, Long> {

    @EntityGraph(attributePaths = {"classePrincipal", "subClasse"})
    Optional<Cnae> findByCnae(String cnae);
}
