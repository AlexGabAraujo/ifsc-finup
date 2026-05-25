import { Component, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { HighchartsChartComponent } from 'highcharts-angular';
import { CardInfo } from "../../shared/components/cards/card-info/card-info";
import { CardChart } from '../../shared/components/cards/card-chart/card-chart';
import { CardLine } from '../../shared/components/cards/card-line/card-line';
import * as Highcharts from 'highcharts';
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
  updateFlag: boolean = false;
  dadosGerais: any;

  chartOptions: Highcharts.Options = {
    chart: { type: 'line', backgroundColor: 'transparent' },
    title: { text: undefined },
    xAxis: { categories: [] },
    yAxis: { title: { text: undefined } },
    series: []
  };

  pieChartOptions: Highcharts.Options = {
    chart: { type: 'pie', backgroundColor: 'transparent' },
    title: { text: undefined },
    series: []
  };

  // === transações + filtro ===
  ultimasTransacoes = signal<TransacoesRecentes[]>([]);
  categorias = signal<{ id: number; nome: string }[]>([]);
  categoriaIdSelecionada: number | null = null;

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