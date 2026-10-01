package com.finup.cnpjs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CnpjRepository extends JpaRepository<Cnpj, Long> {

    Optional<Cnpj> findByCnpj(String cnpj);
}
