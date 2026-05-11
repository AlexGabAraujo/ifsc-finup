package com.finup.categoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByPessoaFisicaId(Long pessoaFisicaId);



}
