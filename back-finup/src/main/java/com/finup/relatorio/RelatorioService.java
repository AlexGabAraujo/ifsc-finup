package com.finup.relatorio;

import com.finup.relatorio.dto.RelatorioGraficoBarrasResponse;
import com.finup.relatorio.dto.RelatorioGraficoPizzaResponse;
import com.finup.relatorio.dto.RelatorioResumoResponse;
import com.finup.relatorio.dto.RelatorioTopCategoriasMesResponse;
import com.finup.transacao.TipoGasto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RelatorioService {

    @Autowired
    private RelatorioRepository relatorioRepository;

    // --- Utilitário de período ---

    private LocalDateTime[] resolverPeriodo(String periodo) {
        LocalDate hoje = LocalDate.now();
        return switch (periodo) {
            case "MES_ATUAL" -> new LocalDateTime[]{
                    hoje.withDayOfMonth(1).atStartOfDay(),
                    hoje.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59)
            };
            case "MES_ANTERIOR" -> {
                LocalDate mesAnterior = hoje.minusMonths(1);
                yield new LocalDateTime[]{
                        mesAnterior.withDayOfMonth(1).atStartOfDay(),
                        mesAnterior.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59)
                };
            }
            case "ULTIMOS_3_MESES" -> new LocalDateTime[]{
                    hoje.minusMonths(3).atStartOfDay(),
                    hoje.atTime(23, 59, 59)
            };
            case "ULTIMOS_6_MESES" -> new LocalDateTime[]{
                    hoje.minusMonths(6).atStartOfDay(),
                    hoje.atTime(23, 59, 59)
            };
            case "ANO_ATUAL" -> new LocalDateTime[]{
                    hoje.withDayOfYear(1).atStartOfDay(),
                    hoje.atTime(23, 59, 59)
            };
            default -> new LocalDateTime[]{
                    hoje.minusYears(5).atStartOfDay(),
                    hoje.atTime(23, 59, 59)
            };
        };
    }

    // --- Métodos públicos ---

    @Transactional(readOnly = true)
    public RelatorioResumoResponse buscarResumo(Long pessoaFisicaId, String periodo) {
        LocalDateTime[] intervalo = resolverPeriodo(periodo);
        LocalDateTime inicio = intervalo[0];
        LocalDateTime fim = intervalo[1];

        BigDecimal receitas = relatorioRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim);
        receitas = receitas != null ? receitas : BigDecimal.ZERO;

        BigDecimal despesas = relatorioRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim);
        despesas = despesas != null ? despesas : BigDecimal.ZERO;

        BigDecimal saldo = receitas.subtract(despesas);

        Long totalTransacoes = relatorioRepository.contarTransacoes(pessoaFisicaId, inicio, fim);
        totalTransacoes = totalTransacoes != null ? totalTransacoes : 0L;

        return new RelatorioResumoResponse(receitas, despesas, saldo, totalTransacoes);
    }

    @Transactional(readOnly = true)
    public RelatorioGraficoBarrasResponse buscarGraficoBarras(Long pessoaFisicaId, String periodo) {
        List<LocalDate> mesesBase = resolverMeses(periodo);

        List<String> meses = new ArrayList<>();
        List<BigDecimal> receitas = new ArrayList<>();
        List<BigDecimal> despesas = new ArrayList<>();

        for (LocalDate mes : mesesBase) {
            LocalDateTime inicio = mes.withDayOfMonth(1).atStartOfDay();
            LocalDateTime fim = mes.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

            String nomeMes = mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR")).toUpperCase().replace(".", "");
            meses.add(nomeMes);

            BigDecimal totalReceita = relatorioRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.CREDITO, inicio, fim);
            receitas.add(totalReceita != null ? totalReceita : BigDecimal.ZERO);

            BigDecimal totalDespesa = relatorioRepository.somarPorPeriodo(pessoaFisicaId, TipoGasto.DEBITO, inicio, fim);
            despesas.add(totalDespesa != null ? totalDespesa : BigDecimal.ZERO);
        }

        return new RelatorioGraficoBarrasResponse(meses, receitas, despesas);
    }

    @Transactional(readOnly = true)
    public List<RelatorioGraficoPizzaResponse> buscarGraficoPizza(Long pessoaFisicaId, String periodo) {
        LocalDateTime[] intervalo = resolverPeriodo(periodo);
        return relatorioRepository.buscarGastosPorCategoria(pessoaFisicaId, intervalo[0], intervalo[1]);
    }

    @Transactional(readOnly = true)
    public List<RelatorioTopCategoriasMesResponse> buscarTopCategoriasPorMes(Long pessoaFisicaId, String periodo) {
        LocalDateTime[] intervalo = resolverPeriodo(periodo);
        List<LocalDate> mesesBase = resolverMeses(periodo);

        List<String> top5 = relatorioRepository.buscarTop5Categorias(pessoaFisicaId, intervalo[0], intervalo[1]);

        List<String> nomesMeses = mesesBase.stream()
                .map(m -> m.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR")).toUpperCase().replace(".", ""))
                .toList();

        List<RelatorioTopCategoriasMesResponse> resultado = new ArrayList<>();

        for (String categoria : top5) {
            List<BigDecimal> valores = new ArrayList<>();
            for (LocalDate mes : mesesBase) {
                LocalDateTime inicio = mes.withDayOfMonth(1).atStartOfDay();
                LocalDateTime fim = mes.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
                BigDecimal valor = relatorioRepository.somarPorCategoriaPeriodo(pessoaFisicaId, categoria, inicio, fim);
                valores.add(valor != null ? valor : BigDecimal.ZERO);
            }
            resultado.add(new RelatorioTopCategoriasMesResponse(categoria, nomesMeses, valores));
        }

        return resultado;
    }

    // --- Utilitário de meses ---

    private List<LocalDate> resolverMeses(String periodo) {
        LocalDate hoje = LocalDate.now();
        List<LocalDate> meses = new ArrayList<>();

        switch (periodo) {
            case "MES_ATUAL" -> meses.add(hoje);
            case "MES_ANTERIOR" -> meses.add(hoje.minusMonths(1));
            case "ULTIMOS_3_MESES" -> {
                for (int i = 2; i >= 0; i--) meses.add(hoje.minusMonths(i));
            }
            case "ULTIMOS_6_MESES" -> {
                for (int i = 5; i >= 0; i--) meses.add(hoje.minusMonths(i));
            }
            case "ANO_ATUAL" -> {
                int mesAtual = hoje.getMonthValue();
                for (int i = 1; i <= mesAtual; i++) meses.add(hoje.withMonth(i));
            }
            default -> {
                for (int i = 11; i >= 0; i--) meses.add(hoje.minusMonths(i));
            }
        }

        return meses;
    }
}
