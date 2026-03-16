package com.finup.pessoaFisica;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaFisicaRepository extends JpaRepository<PessoaFisica, Long> {

    boolean existsByCpf(String cpf);
}
