import * as Highcharts from 'highcharts';
import * as XLSX from 'xlsx';
import { GraficoLinha } from '../../core/services/dashboard.service';
import { TipoGasto } from '../../shared/models/transacao.models';
import { TopCategoriasMes, RelatorioResumo, RelatorioGraficoBarras, RelatorioPizza } from '../../core/services/relatorio.service';

// --- Tipos ---

export type PeriodoRelatorio =
  | 'MES_ATUAL'
  | 'MES_ANTERIOR'
  | 'ULTIMOS_3_MESES'
  | 'ULTIMOS_6_MESES'
  | 'ANO_ATUAL'
  | 'ANO';

export interface LinhaCSV {
  Data: string;
  Categoria: string;
  Tipo: string;
  'Forma de Pagamento': string;
  'Valor (R$)': string;
}

// --- Funções puras ---

export function calcularSaldoAcumulado(receitas: number[], despesas: number[]): number[] {
  return receitas.map((r, i) => r - despesas[i]);
}

export function corSaldo(saldo: number): string {
  return saldo < 0 ? '#f43f5e' : '#10b981';
}

export function gerarLinhasCSV(transacoes: LinhaCSV[]): string {
  const cabecalho = 'Data,Categoria,Tipo,Forma de Pagamento,Valor (R$)';
  const linhas = transacoes.map(
    (t) => `${t.Data},${t.Categoria},${t.Tipo},${t['Forma de Pagamento']},${t['Valor (R$)']}`
  );
  return [cabecalho, ...linhas].join('\n');
}

export function nomeArquivoCSV(periodo: PeriodoRelatorio): string {
  return `relatorio-finup-${periodo}.csv`;
}

export function mapearOpcoesBarras(dados: GraficoLinha): Highcharts.Options {
  return {
    chart: { type: 'column', backgroundColor: 'transparent' },
    title: { text: undefined },
    xAxis: { categories: dados.categoriasX },
    yAxis: { title: { text: undefined }, gridLineColor: '#f3f4f6' },
    tooltip: {
      shared: true,
      useHTML: true,
      pointFormat:
        '<span style="color:{series.color}">●</span> {series.name}: <b>R$ {point.y:.2f}</b><br/>',
    },
    plotOptions: { column: { grouping: true, borderRadius: 4 } },
    credits: { enabled: false },
    series: [
      { name: 'Receitas', type: 'column', data: dados.seriesReceita, color: '#10b981' },
      { name: 'Despesas', type: 'column', data: dados.seriesDespesa, color: '#f43f5e' },
    ] as Highcharts.SeriesOptionsType[],
  };
}

export function estadoBotaoPaginacao(
  paginaAtual: number,
  totalPages: number
): { anteriorDesabilitado: boolean; proximoDesabilitado: boolean } {
  return {
    anteriorDesabilitado: paginaAtual === 0,
    proximoDesabilitado: paginaAtual >= totalPages - 1,
  };
}

export function corTransacao(tipoGasto: TipoGasto): string {
  return tipoGasto === 'CREDITO' ? 'text-graf1' : 'text-graf2';
}

export function mapearOpcoesRankingCategorias(dados: { nome: string; valor: number }[]): Highcharts.Options {
  const ordenado = [...dados].sort((a, b) => b.valor - a.valor);
  return {
    chart: { type: 'bar', backgroundColor: 'transparent' },
    title: { text: undefined },
    xAxis: { categories: ordenado.map((d) => d.nome) },
    yAxis: { title: { text: undefined }, gridLineColor: '#f3f4f6' },
    tooltip: {
      useHTML: true,
      pointFormat: '<b>R$ {point.y:.2f}</b>',
    },
    plotOptions: { bar: { borderRadius: 4, colorByPoint: false } },
    credits: { enabled: false },
    series: [
      {
        name: 'Gastos',
        type: 'bar',
        data: ordenado.map((d) => d.valor),
        color: '#6366f1',
      },
    ] as Highcharts.SeriesOptionsType[],
  };
}

export function gerarExcel(
  transacoes: LinhaCSV[],
  graficoBarras: RelatorioGraficoBarras | null,
  rankingCategorias: RelatorioPizza[],
  resumo: RelatorioResumo | null,
  periodo: PeriodoRelatorio
): void {
  const wb = XLSX.utils.book_new();

  // Aba 1: Transações
  const abaTransacoes = XLSX.utils.json_to_sheet(transacoes);
  XLSX.utils.book_append_sheet(wb, abaTransacoes, 'Transações');

  // Aba 2: Resumo por Mês
  if (graficoBarras) {
    const dadosMeses = graficoBarras.meses.map((mes, i) => ({
      Mês: mes,
      'Receitas (R$)': Number(graficoBarras.receitas[i]).toFixed(2),
      'Despesas (R$)': Number(graficoBarras.despesas[i]).toFixed(2),
      'Saldo (R$)': (Number(graficoBarras.receitas[i]) - Number(graficoBarras.despesas[i])).toFixed(2),
    }));
    const abaMeses = XLSX.utils.json_to_sheet(dadosMeses);
    XLSX.utils.book_append_sheet(wb, abaMeses, 'Resumo por Mês');
  }

  // Aba 3: Ranking de Categorias
  if (rankingCategorias.length > 0) {
    const dadosRanking = [...rankingCategorias]
      .sort((a, b) => Number(b.valor) - Number(a.valor))
      .map((r) => ({ Categoria: r.nome, 'Total Gasto (R$)': Number(r.valor).toFixed(2) }));
    const abaRanking = XLSX.utils.json_to_sheet(dadosRanking);
    XLSX.utils.book_append_sheet(wb, abaRanking, 'Ranking de Categorias');
  }

  // Aba 4: Resumo Geral
  if (resumo) {
    const dadosResumo = [
      { Métrica: 'Total Receitas', 'Valor (R$)': Number(resumo.totalReceitas).toFixed(2) },
      { Métrica: 'Total Despesas', 'Valor (R$)': Number(resumo.totalDespesas).toFixed(2) },
      { Métrica: 'Saldo', 'Valor (R$)': Number(resumo.saldo).toFixed(2) },
      { Métrica: 'Total de Transações', 'Valor (R$)': String(resumo.totalTransacoes) },
    ];
    const abaResumo = XLSX.utils.json_to_sheet(dadosResumo);
    XLSX.utils.book_append_sheet(wb, abaResumo, 'Resumo Geral');
  }

  XLSX.writeFile(wb, `relatorio-finup-${periodo}.xlsx`);
}

export function mapearOpcoesTopCategorias(dados: TopCategoriasMes[]): Highcharts.Options {
  const meses = dados.length > 0 ? dados[0].meses : [];
  return {
    chart: { type: 'line', backgroundColor: 'transparent' },
    title: { text: undefined },
    xAxis: { categories: meses },
    yAxis: { title: { text: undefined }, gridLineColor: '#f3f4f6' },
    tooltip: {
      shared: true,
      useHTML: true,
      pointFormat:
        '<span style="color:{series.color}">●</span> {series.name}: <b>R$ {point.y:.2f}</b><br/>',
    },
    plotOptions: { line: { marker: { enabled: true, radius: 4 } } },
    credits: { enabled: false },
    series: dados.map((cat) => ({
      name: cat.categoria,
      type: 'line' as const,
      data: cat.valores,
    })) as Highcharts.SeriesOptionsType[],
  };
}
