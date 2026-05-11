package com.finup.categoria.UtilCategoria;

import com.finup.categoria.PeriodoFiltro;

import java.time.LocalDateTime;
import java.time.YearMonth;

public class ComparacaoFiltro {
    public record IntervaloPeriodo(LocalDateTime inicio, LocalDateTime fim) {}

    /**
     * Calcula intervalo de tempo do filtro
     */
    public static IntervaloPeriodo calcularIntervalo(PeriodoFiltro periodo) {
        LocalDateTime agora = LocalDateTime.now();

        switch (periodo) {
            case MES_ATUAL: {
                YearMonth ym = YearMonth.from(agora);
                return new IntervaloPeriodo(
                        ym.atDay(1).atStartOfDay(),
                        ym.atEndOfMonth().atTime(23, 59, 59)
                );
            }
            case MES_ANTERIOR: {
                YearMonth ym = YearMonth.from(agora).minusMonths(1);
                return new IntervaloPeriodo(
                        ym.atDay(1).atStartOfDay(),
                        ym.atEndOfMonth().atTime(23, 59, 59)
                );
            }
            case ULTIMOS_3_MESES: {
                YearMonth fim = YearMonth.from(agora);
                YearMonth inicio = fim.minusMonths(2);
                return new IntervaloPeriodo(
                        inicio.atDay(1).atStartOfDay(),
                        fim.atEndOfMonth().atTime(23, 59, 59)
                );
            }
            case ULTIMOS_6_MESES: {
                YearMonth fim = YearMonth.from(agora);
                YearMonth inicio = fim.minusMonths(5);
                return new IntervaloPeriodo(
                        inicio.atDay(1).atStartOfDay(),
                        fim.atEndOfMonth().atTime(23, 59, 59)
                );
            }
            case ANO_ATUAL: {
                int ano = agora.getYear();
                return new IntervaloPeriodo(
                        LocalDateTime.of(ano, 1, 1, 0, 0),
                        LocalDateTime.of(ano, 12, 31, 23, 59, 59)
                );
            }
            default:
                throw new IllegalArgumentException("Período inválido: " + periodo);
        }
    }

    /**
     * Utilizado para comparação com o mes anterior
     */
    public static IntervaloPeriodo intervaloPeriodoAnterior(PeriodoFiltro periodo) {
        LocalDateTime agora = LocalDateTime.now();

        switch (periodo) {
            case MES_ATUAL: {
                YearMonth ym = YearMonth.from(agora).minusMonths(1);
                return new IntervaloPeriodo(
                        ym.atDay(1).atStartOfDay(),
                        ym.atEndOfMonth().atTime(23, 59, 59)
                );
            }
            case MES_ANTERIOR: {
                YearMonth ym = YearMonth.from(agora).minusMonths(2);
                return new IntervaloPeriodo(
                        ym.atDay(1).atStartOfDay(),
                        ym.atEndOfMonth().atTime(23, 59, 59)
                );
            }
            case ULTIMOS_3_MESES: {
                YearMonth fim = YearMonth.from(agora).minusMonths(3);
                YearMonth inicio = fim.minusMonths(2);
                return new IntervaloPeriodo(
                        inicio.atDay(1).atStartOfDay(),
                        fim.atEndOfMonth().atTime(23, 59, 59)
                );
            }
            case ULTIMOS_6_MESES: {
                YearMonth fim = YearMonth.from(agora).minusMonths(6);
                YearMonth inicio = fim.minusMonths(5);
                return new IntervaloPeriodo(
                        inicio.atDay(1).atStartOfDay(),
                        fim.atEndOfMonth().atTime(23, 59, 59)
                );
            }
            case ANO_ATUAL: {
                int ano = agora.getYear() - 1;
                return new IntervaloPeriodo(
                        LocalDateTime.of(ano, 1, 1, 0, 0),
                        LocalDateTime.of(ano, 12, 31, 23, 59, 59)
                );
            }
            default:
                throw new IllegalArgumentException("Período inválido: " + periodo);
        }
    }
}
