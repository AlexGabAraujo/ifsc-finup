package com.finup.pessoaFisica;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PessoaFisicaRepository extends JpaRepository<PessoaFisica, Long> {

    boolean existsByCpf(String cpf);
}
