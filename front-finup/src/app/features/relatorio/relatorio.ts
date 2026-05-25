import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import Highcharts from 'highcharts';
import { HighchartsChartComponent } from 'highcharts-angular';

import {
  RelatorioService,
  FiltroRelatorio,
  RelatorioResumo,
  RelatorioGraficoBarras,
  RelatorioPizza,
  TopCategoriasMes,
} from '../../core/services/relatorio.service';
import { TransacaoPageResponse } from '../../shared/models/transacao.models';
import { CardInfo } from '../../shared/components/cards/card-info/card-info';
import { CardChart } from '../../shared/components/cards/card-chart/card-chart';
import {
  mapearOpcoesBarras,
  calcularSaldoAcumulado,
  corSaldo,
  gerarLinhasCSV,
  nomeArquivoCSV,
  corTransacao,
  estadoBotaoPaginacao,
  mapearOpcoesRankingCategorias,
  mapearOpcoesTopCategorias,
  gerarExcel,
  LinhaCSV,
} from './relatorio.utils';

@Component({
  selector: 'app-relatorio',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, FormsModule, HighchartsChartComponent, CardInfo, CardChart],
  templateUrl: './relatorio.html',
})
export class Relatorio implements OnInit {
  private relatorioService = inject(RelatorioService);

  // --- Estado ---

  filtro: FiltroRelatorio = { periodo: 'ULTIMOS_6_MESES', tipo: 'TODOS' };
  resumo: RelatorioResumo | null = null;
  graficoBarras: RelatorioGraficoBarras | null = null;
  graficoPizza: RelatorioPizza[] = [];
  rankingCategorias: RelatorioPizza[] = [];
  topCategoriasMes: TopCategoriasMes[] = [];
  transacoes: TransacaoPageResponse | null = null;
  paginaAtual = 0;
  loading = false;

  // --- Highcharts ---

  Highcharts: typeof Highcharts = Highcharts;
  updateFlag = false;

  barrasOptions: Highcharts.Options = {};
  areaOptions: Highcharts.Options = {};
  pizzaOptions: Highcharts.Options = {};
  rankingOptions: Highcharts.Options = {};
  topCategoriasOptions: Highcharts.Options = {};

  // --- Ciclo de vida ---

  ngOnInit(): void {
    this.carregarDados();
  }

  // --- Carregamento de dados ---

  carregarDados(): void {
    this.loading = true;
    const params = this.relatorioService.periodoParaParams(this.filtro.periodo);

    forkJoin({
      resumo: this.relatorioService.carregarResumo(this.filtro.periodo).pipe(catchError(() => of(null))),
      barras: this.relatorioService.carregarGraficoBarrasRelatorio(this.filtro.periodo).pipe(catchError(() => of(null))),
      pizza: this.relatorioService.carregarGraficoPizzaRelatorio(this.filtro.periodo).pipe(catchError(() => of(null))),
      topCategorias: this.relatorioService.carregarTopCategoriasPorMes(this.filtro.periodo).pipe(catchError(() => of(null))),
      transacoes: this.relatorioService
        .carregarTransacoes(
          { mes: params.mes ?? null, ano: params.ano ?? null, categoriaId: null, categoriaType: null },
          this.paginaAtual
        )
        .pipe(catchError(() => of(null))),
    }).subscribe((res) => {
      this.resumo = res.resumo;
      this.graficoBarras = res.barras;
      this.graficoPizza = res.pizza ?? [];
      this.rankingCategorias = res.pizza ?? [];
      this.topCategoriasMes = res.topCategorias ?? [];
      this.transacoes = res.transacoes;
      this.loading = false;

      this.construirGraficos();
    });
  }

  // --- Construção dos gráficos ---

  private construirGraficos(): void {
    if (this.graficoBarras) {
      this.barrasOptions = mapearOpcoesBarras({
        categoriasX: this.graficoBarras.meses,
        seriesReceita: this.graficoBarras.receitas,
        seriesDespesa: this.graficoBarras.despesas,
      });
      this.areaOptions = this.construirOpcoesArea(this.graficoBarras);
    }

    if (this.graficoPizza.length > 0) {
      this.pizzaOptions = this.construirOpcoesPizza(this.graficoPizza);
    }

    if (this.rankingCategorias.length > 0) {
      this.rankingOptions = mapearOpcoesRankingCategorias(
        this.rankingCategorias.map((r) => ({ nome: r.nome, valor: Number(r.valor) }))
      );
    }

    if (this.topCategoriasMes.length > 0) {
      this.topCategoriasOptions = mapearOpcoesTopCategorias(this.topCategoriasMes);
    }

    this.updateFlag = !this.updateFlag;
  }

  private construirOpcoesArea(dados: RelatorioGraficoBarras): Highcharts.Options {
    const saldo = calcularSaldoAcumulado(
      dados.receitas.map(Number),
      dados.despesas.map(Number)
    );
    const saldoTotal = saldo.reduce((acc, v) => acc + v, 0);
    const cor = corSaldo(saldoTotal);

    return {
      chart: { type: 'area', backgroundColor: 'transparent' },
      title: { text: '' },
      xAxis: { categories: dados.meses },
      yAxis: { title: { text: undefined }, gridLineColor: '#f3f4f6' },
      tooltip: {
        shared: true,
        useHTML: true,
        pointFormat:
          '<span style="color:{series.color}">●</span> {series.name}: <b>R$ {point.y:.2f}</b><br/>',
      },
      plotOptions: { area: { fillOpacity: 0.2 } },
      credits: { enabled: false },
      series: [
        { name: 'Saldo', type: 'area', data: saldo, color: cor, fillColor: cor },
      ] as Highcharts.SeriesOptionsType[],
    };
  }

  private construirOpcoesPizza(dados: RelatorioPizza[]): Highcharts.Options {
    return {
      chart: { type: 'pie', backgroundColor: 'transparent' },
      title: { text: '' },
      legend: {
        enabled: true,
        layout: 'vertical',
        align: 'left',
        verticalAlign: 'middle',
        itemStyle: { color: '#374151', fontSize: '12px', fontWeight: '600' },
        itemMarginBottom: 10,
        symbolRadius: 4,
      },
      plotOptions: {
        pie: {
          innerSize: '60%',
          borderRadius: 0,
          dataLabels: { enabled: false },
          showInLegend: true,
          states: { hover: { brightness: 0.1 } },
        },
      },
      tooltip: {
        enabled: true,
        useHTML: true,
        headerFormat: '',
        backgroundColor: '#ffffff',
        borderWidth: 1,
        borderColor: '#e5e7eb',
        shadow: true,
        pointFormat: '<b>{point.name}</b>: {point.percentage:.1f}%',
      },
      credits: { enabled: false },
      series: [
        {
          type: 'pie',
          data: dados.map((item) => ({ name: item.nome, y: Number(item.valor) })),
        },
      ] as Highcharts.SeriesOptionsType[],
    };
  }

  // --- Filtros ---

  onFiltroChange(): void {
    this.paginaAtual = 0;
    this.carregarDados();
  }

  // --- Paginação ---

  paginaAnterior(): void {
    if (this.paginaAtual > 0) {
      this.paginaAtual--;
      this.carregarDados();
    }
  }

  proximaPagina(): void {
    const total = this.transacoes?.totalPages ?? 0;
    if (this.paginaAtual < total - 1) {
      this.paginaAtual++;
      this.carregarDados();
    }
  }

  get estadoPaginacao() {
    return estadoBotaoPaginacao(this.paginaAtual, this.transacoes?.totalPages ?? 0);
  }

  // --- CSV ---

  baixarExcel(): void {
    const linhas: LinhaCSV[] = (this.transacoes?.content ?? []).map((t) => ({
      Data: t.dataTransacao,
      Categoria: String(t.classePrincipalId ?? t.subClasseId ?? ''),
      Tipo: t.tipoGasto === 'CREDITO' ? 'Receita' : 'Despesa',
      'Forma de Pagamento': t.tipoPagamento,
      'Valor (R$)': t.valor.toFixed(2),
    }));

    gerarExcel(linhas, this.graficoBarras, this.rankingCategorias, this.resumo, this.filtro.periodo);
  }

  // --- Utilitários expostos ao template ---

  corTransacao = corTransacao;
}
