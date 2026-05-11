package com.finup.dash;

import com.finup.dash.dto.DetailGraficoDashResponse;
import com.finup.dash.dto.DetailGraficoPizzaResponse;
import com.finup.transacao.TipoGasto;
import com.finup.transacao.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DashRepository extends JpaRepository<Transacao, Long> {

    @Query("SELECT SUM(t.valor) FROM Transacao t " +
            "WHERE t.pessoaFisica.id = :pessoaFisicaId " +
            "AND t.tipoGasto = :tipo " +
            "AND t.dataInsercao BETWEEN :inicio AND :fim")
    BigDecimal somarPorPeriodo(Long pessoaFisicaId, TipoGasto tipo, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT new com.finup.dash.dto.DetailGraficoPizzaResponse(" +
            "  COALESCE(sc.nome, cp.nome), " +
            "  SUM(t.valor)) " +
            "FROM Transacao t " +
            "LEFT JOIN t.subClasse sc " +
            "LEFT JOIN t.classePrincipal cp " +
            "WHERE t.pessoaFisica.id = :pessoaFisicaId " +
            "AND t.tipoGasto = com.finup.transacao.TipoGasto.DEBITO " +
            "AND t.dataInsercao BETWEEN :inicio AND :fim " +
            "GROUP BY COALESCE(sc.nome, cp.nome)")
    List<DetailGraficoPizzaResponse> buscarGastosPorCategoria(Long pessoaFisicaId, LocalDateTime inicio, LocalDateTime fim);

    List<Transacao> findTop6ByPessoaFisicaIdOrderByDataInsercaoDesc(Long pessoaFisicaId);

    List<Transacao> findTop6ByPessoaFisicaIdAndCategoriaIdOrderByDataInsercaoDesc(
            Long pessoaFisicaId, Long categoriaId
    );

}
