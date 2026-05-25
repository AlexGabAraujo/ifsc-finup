import { ChangeDetectorRef, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CardChart } from "../../shared/components/cards/card-chart/card-chart";
import { CardInfo } from "../../shared/components/cards/card-info/card-info";
import * as Highcharts from 'highcharts';
import { HighchartsChartComponent } from 'highcharts-angular';
import { CommonModule } from '@angular/common';
import { TelaCategoriaService, DetailCategoriaOrcamentoResponse, PeriodoFiltro, DetailGraficoCategoriaResponse, DetailCategoriaCardResponse } from '../../core/services/telaCategoria.service';
import { CardCategoriaModel } from './cardCategoria/card-categoria-model/card-categoria-model';
import { TransacaoFormModal } from '../transacoes/transacao-form-modal/transacao-form-modal';
import { CategoriaFormModal } from './cardCategoria/categoria-form-modal/categoria-form-modal';


// Cores para o gráfico, em ordem cíclica, futuramente modificar para a cor ser definida pela cor da categoria vinda do backend.
const CORES_GRAFICO = ['#6366f1', '#10b981', '#f59e0b', '#f43f5e', '#94a3b8', '#0ea5e9', '#a855f7'];


@Component({
  selector: 'app-categoria',
  standalone: true,
  imports: [CardCategoriaModel, CardChart, CardInfo, HighchartsChartComponent, CommonModule, TransacaoFormModal, CategoriaFormModal],
  templateUrl: './categoria.html',
})
export class Categoria implements OnInit {
  private categoriaService = inject(TelaCategoriaService);
  private cdr = inject(ChangeDetectorRef);

  dados = signal<DetailCategoriaOrcamentoResponse | null>(null);
  carregando = signal(true);
  erro = signal<string | null>(null);
  periodo = signal<PeriodoFiltro>('MES_ATUAL');
  modalAberto = false;
  busca = signal('');

  // Lista filtrada — recalcula automaticamente quando busca() ou dados() mudam
  categoriasFiltradas = computed(() => {
    const termo = this.busca().toLowerCase().trim();
    const lista = this.dados()?.categorias ?? [];

    if (!termo) return lista;   // sem termo = mostra tudo

    return lista.filter((c) =>
      c.nome.toLowerCase().includes(termo) ||
      (c.descricao?.toLowerCase().includes(termo) ?? false)
    );
  });

  categoriaSelecionadaId: number | null = null;

  modalNovaCategoriaAberto = signal(false);

  abrirModalNovaCategoria(): void {
    this.modalNovaCategoriaAberto.set(true);
  }

  fecharModalNovaCategoria(): void {
    this.modalNovaCategoriaAberto.set(false);
  }

  Highcharts: typeof Highcharts = Highcharts;
  updateFlag = false;

  chartOptions: Highcharts.Options = {
    chart: {
      type: 'line',
      backgroundColor: 'transparent',
      reflow: true,
      spacingLeft: 0,
      spacingRight: 0,
    },
    title: { text: undefined },
    xAxis: {
      categories: [],
      crosshair: true,
    },
    yAxis: {
      title: { text: undefined },
      gridLineColor: '#f3f4f6',
    },
    tooltip: {
      shared: true,
      useHTML: true,
      pointFormat:
        '<span style="color:{series.color}">●</span> {series.name}: <b>R$ {point.y:.2f}</b><br/>',
    },
    plotOptions: { line: { marker: { enabled: true, symbol: 'circle' } } },
    credits: { enabled: false },
    series: [],
  };

  ngOnInit(): void {
    this.carregarDados();
    this.carregarGrafico();

  }

  carregarDados(): void {

    this.carregando.set(true);
    this.erro.set(null);

    this.categoriaService.getOrcamentoTotal(this.periodo()).subscribe({
      next: (res) => {

        this.dados.set(res);
        this.carregando.set(false);
      },
      error: (err) => {

        this.erro.set('Erro ao carregar categorias');
        this.carregando.set(false);
        console.error(err);
      },
    });
  }

  carregarGrafico(): void {
    this.categoriaService.getGraficoLinha('ULTIMOS_6_MESES').subscribe({
      next: (res) => this.atualizarGrafico(res),
      error: (err) => console.error('Erro ao carregar gráfico:', err),
    });
  }

  private atualizarGrafico(res: DetailGraficoCategoriaResponse): void {
    this.updateFlag = false;
    this.cdr.markForCheck();

    setTimeout(() => {
      this.chartOptions = {
        ...this.chartOptions,
        xAxis: {
          ...this.chartOptions.xAxis,
          categories: res.labels,
        },
        series: res.datasets.map((ds, i) => ({
          type: 'line',
          name: ds.nome,
          data: ds.data,
          color: CORES_GRAFICO[i % CORES_GRAFICO.length],
        })),
      };
      this.updateFlag = true;
      this.cdr.markForCheck();
    }, 0);
  }

  trocarPeriodo(novo: PeriodoFiltro): void {
    this.periodo.set(novo);
    this.carregarDados();
  }

  formatarMoeda(v: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(v ?? 0);
  }

  abrirModalTransacao(categoriaId: number): void {
    this.categoriaSelecionadaId = categoriaId;
    this.modalAberto = true;
  }

  fecharModal(): void {
    this.modalAberto = false;
    this.categoriaSelecionadaId = null;
  }

  aoSalvarTransacao(): void {
    this.fecharModal();
    this.carregarDados();
    this.carregarGrafico();
  }

  modalConfirmacaoAberto = signal(false);
  categoriaParaExcluir = signal<DetailCategoriaCardResponse | null>(null);
  excluindo = signal(false);

  abrirConfirmacaoExclusao(categoria: DetailCategoriaCardResponse): void {
    this.categoriaParaExcluir.set(categoria);
    this.modalConfirmacaoAberto.set(true);
  }

  cancelarExclusao(): void {
    this.modalConfirmacaoAberto.set(false);
    this.categoriaParaExcluir.set(null);
  }

  confirmarExclusao(): void {
    const categoria = this.categoriaParaExcluir();
    if (!categoria) return;

    this.excluindo.set(true);
    this.categoriaService.deletar(categoria.id).subscribe({
      next: () => {
        this.excluindo.set(false);
        this.modalConfirmacaoAberto.set(false);
        this.categoriaParaExcluir.set(null);
        this.carregarDados();
        this.carregarGrafico();
      },
      error: (err) => {
        this.excluindo.set(false);
        alert('Erro ao excluir categoria: ' + (err.error?.message ?? 'Tente novamente.'));
      },
    });
  }

  aoSalvarCategoria(): void {
    this.fecharModalNovaCategoria();
    this.carregarDados();
    this.carregarGrafico();
  }

}
