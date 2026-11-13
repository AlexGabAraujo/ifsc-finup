package com.finup.pluggy.contaBancaria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {
    Optional<ContaBancaria> findByPluggyAccountId(String pluggyAccountId);
    List<ContaBancaria> findAllByItemBancarioId(Long itemBancarioId);
}
