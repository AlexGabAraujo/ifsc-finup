import { Component, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { HighchartsChartComponent } from 'highcharts-angular';
import { CardInfo } from "../../shared/components/cards/card-info/card-info";
import { CardChart } from '../../shared/components/cards/card-chart/card-chart';
import { CardLine } from '../../shared/components/cards/card-line/card-line';
import Highcharts from 'highcharts';
import { DashboardService, TransacoesRecentes } from '../../core/services/dashboard.service';
import { CurrencyPipe, CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CardInfo, CardChart, CardLine, HighchartsChartComponent, CurrencyPipe, CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class Dashboard implements OnInit {

  exibirSaldo: boolean = true;
  Highcharts: typeof Highcharts = Highcharts;
  updateFlag: boolean = false;
  dadosGerais: any;

  // === transações + filtro ===
  ultimasTransacoes = signal<TransacoesRecentes[]>([]);
  categorias = signal<{ id: number; nome: string }[]>([]);
  categoriaIdSelecionada: number | null = null;

  chartOptions: Highcharts.Options = {
    chart: { type: 'line', backgroundColor: 'transparent', reflow: true, spacingLeft: 0, spacingRight: 0 },
    title: { text: '' },
    xAxis: { categories: [], crosshair: true },
    yAxis: { title: { text: undefined }, gridLineColor: '#f3f4f6' },
    tooltip: { shared: true, useHTML: true, pointFormat: '<span style="color:{series.color}">●</span> {series.name}: <b>R$ {point.y:.2f}</b><br/>' },
    plotOptions: { line: { marker: { enabled: true, symbol: 'circle' } } },
    credits: { enabled: false },
    series: [
      { name: 'Receitas', type: 'line', data: [], color: '#10b981' },
      { name: 'Despesas', type: 'line', data: [], color: '#f43f5e' }
    ] as any,
  };

  pieChartOptions: Highcharts.Options = {
    chart: { type: 'pie', backgroundColor: 'transparent' },
    title: { text: '' },
    legend: {
      enabled: true, layout: 'vertical', align: 'left', verticalAlign: 'middle',
      itemStyle: { color: '#374151', fontSize: '12px', fontWeight: '600' },
      itemMarginBottom: 10,
      symbolRadius: 4
    },
    plotOptions: { pie: { innerSize: '60%', borderRadius: 0, dataLabels: { enabled: false }, showInLegend: true, states: { hover: { brightness: 0.1 } } } },
    tooltip: { enabled: true, useHTML: true, headerFormat: '', backgroundColor: '#ffffff', borderWidth: 1, borderColor: '#e5e7eb', shadow: true, pointFormat: '<b>{point.name}</b>: {point.percentage:.1f}%' },
    series: [{ type: 'pie', data: [] }] as any
  };

  constructor(private dashboardService: DashboardService, private cdr: ChangeDetectorRef) { }

  ngOnInit(): void {
    this.carregarDados();
    this.carregarTransacoes();          // ← carrega transações (filtro = null = todas)

    // Carrega categorias pro select
    this.dashboardService.listarCategorias().subscribe({
      next: (res) => this.categorias.set(res.categorias),
      error: (err) => console.error('Erro categorias:', err),
    });
  }

  carregarDados() {
    this.dashboardService.getGraficoLinha().subscribe({
      next: (res: any) => {
        this.chartOptions.xAxis = { categories: res.categoriasX };
        this.chartOptions.series = [
          { name: 'Receitas', data: res.seriesReceita, type: 'line', color: '#10b981' },
          { name: 'Despesas', data: res.seriesDespesa, type: 'line', color: '#f43f5e' }
        ] as any;
        this.updateFlag = true;
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('Erro ao carregar valores:', err)
    });

    this.dashboardService.getGraficoPizza().subscribe({
      next: (res: any[]) => {
        const dadosPizza = res.map(item => ({
          name: item.nome,
          y: item.valor,
          color: this.gerarCorAleatoria()
        }));
        this.pieChartOptions.series = [{ type: 'pie', data: dadosPizza }] as any;
        this.updateFlag = true;
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('Erro ao carregar pizza:', err)
    });

    this.dashboardService.getInfoDashboard().subscribe({
      next: (res: any) => {
        this.dadosGerais = res;
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('Erro ao carregar cards:', err)
    });

    // 👇 REMOVI o getTransacoesRecentes — agora é só o carregarTransacoes
  }

  carregarTransacoes(): void {
    this.dashboardService.getUltimasTransacoes(this.categoriaIdSelecionada).subscribe({
      next: (lista) => this.ultimasTransacoes.set(lista),
      error: (err) => console.error('Erro transações:', err),
    });
  }

  filtrar(valor: string): void {
    this.categoriaIdSelecionada = valor ? Number(valor) : null;
    this.carregarTransacoes();
  }

  iconeSaldo() {
    this.exibirSaldo = !this.exibirSaldo;
  }

  private gerarCorAleatoria(): string {
    const hex = Math.floor(Math.random() * 16777215).toString(16);
    return '#' + hex.padStart(6, '0');
  }
}