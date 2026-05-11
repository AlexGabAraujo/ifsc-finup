package com.finup.transacao;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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


}