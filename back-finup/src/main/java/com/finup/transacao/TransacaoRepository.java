package com.finup.transacao;

import com.finup.categoria.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    /**
     * Soma o valor de todas as transações de DÉBITO (gasto/saída) de uma
     * categoria em um período. CRÉDITOS são ignorados, pois representam
     * entradas e não devem consumir o orçamento.
     */
    @Query("""
            SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t
            WHERE t.categoria.id = :categoriaId
              AND t.dataInsercao BETWEEN :inicio AND :fim
              AND t.tipoGasto = com.finup.transacao.TipoGasto.DEBITO
            """)
    BigDecimal somarPorCategoriaEPeriodo(
            @Param("categoriaId") Long categoriaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Conta apenas transações de DÉBITO no período (mesma lógica acima).
     */
    @Query("""
            SELECT COUNT(t) FROM Transacao t
            WHERE t.categoria.id = :categoriaId
              AND t.dataInsercao BETWEEN :inicio AND :fim
              AND t.tipoGasto = com.finup.transacao.TipoGasto.DEBITO
            """)
    Long contarPorCategoriaEPeriodo(
            @Param("categoriaId") Long categoriaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    /**
     * Soma TODAS as transações de DÉBITO da pessoa no período
     * (independente de categoria). Útil para totalizadores gerais.
     */
    @Query("""
            SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t
            WHERE t.pessoaFisica.id = :pessoaId
              AND t.dataInsercao BETWEEN :inicio AND :fim
              AND t.tipoGasto = com.finup.transacao.TipoGasto.DEBITO
            """)
    BigDecimal somarTotalPorPeriodo(
            @Param("pessoaId") Long pessoaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Query("""
            SELECT YEAR(t.dataInsercao),
                   MONTH(t.dataInsercao),
                   t.categoria.id,
                   COALESCE(SUM(t.valor), 0)
            FROM Transacao t
            WHERE t.pessoaFisica.id = :pessoaId
              AND t.dataInsercao BETWEEN :inicio AND :fim
              AND t.tipoGasto = com.finup.transacao.TipoGasto.DEBITO
              AND t.categoria.id IS NOT NULL
            GROUP BY YEAR(t.dataInsercao), MONTH(t.dataInsercao), t.categoria.id
            ORDER BY YEAR(t.dataInsercao), MONTH(t.dataInsercao)
            """)
    List<Object[]> somarPorMesECategoria(
            @Param("pessoaId") Long pessoaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM Transacao t WHERE t.categoria.id = :categoriaId")
    void deleteAllByCategoriaId(@Param("categoriaId") Long categoriaId);


    // Listagem paginada com filtros opcionais de período e categoria
    @Query("""
            SELECT t FROM Transacao t
            WHERE t.pessoaFisica.id = :pessoaFisicaId
              AND (:mes IS NULL OR MONTH(t.dataInsercao) = :mes)
              AND (:ano IS NULL OR YEAR(t.dataInsercao) = :ano)
              AND (:categoriaId IS NULL OR
                   (:categoriaType = 'CLASSE_PRINCIPAL' AND t.classePrincipal.id = :categoriaId) OR
                   (:categoriaType = 'SUBCLASSE' AND t.subClasse.id = :categoriaId))
            ORDER BY t.dataInsercao DESC
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
              AND (:mes IS NULL OR MONTH(t.dataInsercao) = :mes)
              AND (:ano IS NULL OR YEAR(t.dataInsercao) = :ano)
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
              AND (:mes IS NULL OR MONTH(t.dataInsercao) = :mes)
              AND (:ano IS NULL OR YEAR(t.dataInsercao) = :ano)
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
              AND (:mes IS NULL OR MONTH(t.dataInsercao) = :mes)
              AND (:ano IS NULL OR YEAR(t.dataInsercao) = :ano)
            """)
    Long countByTipoGastoAndPessoaFisicaId(
            @Param("pessoaFisicaId") Long pessoaFisicaId,
            @Param("tipoGasto") TipoGasto tipoGasto,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

}

