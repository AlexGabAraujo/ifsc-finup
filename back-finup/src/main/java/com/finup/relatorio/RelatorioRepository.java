package com.finup.relatorio;

import com.finup.relatorio.dto.RelatorioGraficoPizzaResponse;
import com.finup.transacao.TipoGasto;
import com.finup.transacao.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface RelatorioRepository extends JpaRepository<Transacao, Long> {

    @Query("SELECT SUM(t.valor) FROM Transacao t WHERE t.pessoaFisica.id = :pessoaFisicaId AND t.tipoGasto = :tipo AND t.dataInsercao BETWEEN :inicio AND :fim")
    BigDecimal somarPorPeriodo(Long pessoaFisicaId, TipoGasto tipo, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT COUNT(t) FROM Transacao t WHERE t.pessoaFisica.id = :pessoaFisicaId AND t.dataInsercao BETWEEN :inicio AND :fim")
    Long contarTransacoes(Long pessoaFisicaId, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT new com.finup.relatorio.dto.RelatorioGraficoPizzaResponse(COALESCE(sc.nome, cp.nome), SUM(t.valor)) FROM Transacao t LEFT JOIN t.subClasse sc LEFT JOIN t.classePrincipal cp WHERE t.pessoaFisica.id = :pessoaFisicaId AND t.tipoGasto = com.finup.transacao.TipoGasto.DEBITO AND t.dataInsercao BETWEEN :inicio AND :fim GROUP BY COALESCE(sc.nome, cp.nome) ORDER BY SUM(t.valor) DESC")
    List<RelatorioGraficoPizzaResponse> buscarGastosPorCategoria(Long pessoaFisicaId, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT COALESCE(sc.nome, cp.nome) FROM Transacao t LEFT JOIN t.subClasse sc LEFT JOIN t.classePrincipal cp WHERE t.pessoaFisica.id = :pessoaFisicaId AND t.tipoGasto = com.finup.transacao.TipoGasto.DEBITO AND t.dataInsercao BETWEEN :inicio AND :fim GROUP BY COALESCE(sc.nome, cp.nome) ORDER BY SUM(t.valor) DESC LIMIT 5")
    List<String> buscarTop5Categorias(Long pessoaFisicaId, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT SUM(t.valor) FROM Transacao t LEFT JOIN t.subClasse sc LEFT JOIN t.classePrincipal cp WHERE t.pessoaFisica.id = :pessoaFisicaId AND t.tipoGasto = com.finup.transacao.TipoGasto.DEBITO AND COALESCE(sc.nome, cp.nome) = :categoria AND t.dataInsercao BETWEEN :inicio AND :fim")
    BigDecimal somarPorCategoriaPeriodo(Long pessoaFisicaId, String categoria, LocalDateTime inicio, LocalDateTime fim);
}
