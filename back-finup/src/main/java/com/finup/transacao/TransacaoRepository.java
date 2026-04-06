package com.finup.transacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    // Listagem paginada com filtros opcionais de período e categoria
    @Query("""
            SELECT t FROM Transacao t
            WHERE t.pessoaFisica.id = :pessoaFisicaId
              AND (:mes IS NULL OR MONTH(t.dataTransacao) = :mes)
              AND (:ano IS NULL OR YEAR(t.dataTransacao) = :ano)
              AND (:categoriaId IS NULL OR
                   (:categoriaType = 'CLASSE_PRINCIPAL' AND t.classePrincipal.id = :categoriaId) OR
                   (:categoriaType = 'SUBCLASSE' AND t.subClasse.id = :categoriaId))
            ORDER BY t.dataTransacao DESC
            """)
    Page<Transacao> findByFilters(
            @Param("pessoaFisicaId") Long pessoaFisicaId,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            @Param("categoriaId") Long categoriaId,
            @Param("categoriaType") String categoriaType,
            Pageable pageable);

    // Soma o valor por tipoGasto para um usuário, com filtros opcionais de mes/ano
    @Query("""
            SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t
            WHERE t.pessoaFisica.id = :pessoaFisicaId
              AND t.tipoGasto = :tipoGasto
              AND (:mes IS NULL OR MONTH(t.dataTransacao) = :mes)
              AND (:ano IS NULL OR YEAR(t.dataTransacao) = :ano)
            """)
    BigDecimal sumValorByTipoGastoAndPessoaFisicaId(
            @Param("pessoaFisicaId") Long pessoaFisicaId,
            @Param("tipoGasto") TipoGasto tipoGasto,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    // Conta total de transações do usuário, com filtros opcionais de mes/ano
    @Query("""
            SELECT COUNT(t) FROM Transacao t
            WHERE t.pessoaFisica.id = :pessoaFisicaId
              AND (:mes IS NULL OR MONTH(t.dataTransacao) = :mes)
              AND (:ano IS NULL OR YEAR(t.dataTransacao) = :ano)
            """)
    Long countByPessoaFisicaId(
            @Param("pessoaFisicaId") Long pessoaFisicaId,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    // Conta transações por tipoGasto para um usuário, com filtros opcionais de mes/ano
    @Query("""
            SELECT COUNT(t) FROM Transacao t
            WHERE t.pessoaFisica.id = :pessoaFisicaId
              AND t.tipoGasto = :tipoGasto
              AND (:mes IS NULL OR MONTH(t.dataTransacao) = :mes)
              AND (:ano IS NULL OR YEAR(t.dataTransacao) = :ano)
            """)
    Long countByTipoGastoAndPessoaFisicaId(
            @Param("pessoaFisicaId") Long pessoaFisicaId,
            @Param("tipoGasto") TipoGasto tipoGasto,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);
}
