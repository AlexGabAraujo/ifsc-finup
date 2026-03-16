import { Component } from '@angular/core';
import { CardCateg } from "../../shared/components/cards/card-categ/card-categ";
import { CardChart } from "../../shared/components/cards/card-chart/card-chart";
import { CardInfo } from "../../shared/components/cards/card-info/card-info";
import Highcharts from 'highcharts';
import { HighchartsChartComponent } from 'highcharts-angular';


@Component({
  selector: 'app-categoria',
  standalone: true,
  imports: [CardCateg, CardChart, CardInfo, HighchartsChartComponent],
  templateUrl: './categoria.html',
  styleUrl: './categoria.css',
})
export class Categoria {

  Highcharts: typeof Highcharts = Highcharts;

  chartOptions: Highcharts.Options = {
    chart: {
      type: 'line',
      backgroundColor: 'transparent',
      reflow: true,
      spacingLeft: 0,
      spacingRight: 0
    },
    title: { text: '' },
    xAxis: {
      categories: ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun'],
      crosshair: true
    },
    yAxis: {
      title: { text: undefined },
      gridLineColor: '#f3f4f6'
    },
    tooltip: { shared: true, useHTML: true, pointFormat: '<span style="color:{series.color}">●</span> {series.name}: <b>R$ {point.y:.2f}</b><br/>' },
    plotOptions: { line: { marker: { enabled: true, symbol: 'circle', } } },
    credits: { enabled: false },
    series: [
      { name: 'Alimentação', type: 'line', data: [1200, 1300, 1100, 1150, 1250, 1200], color: '#6366f1' },
      { name: 'Transporte', type: 'line', data: [850, 800, 900, 750, 800, 800], color: '#10b981' },
      { name: 'Educação', type: 'line', data: [400, 450, 500, 400, 480, 300], color: '#f59e0b' },
      { name: 'Casa', type: 'line', data: [1500, 1200, 1050, 1100, 1350, 1200], color: '#f43f5e' },
      { name: 'Lazer', type: 'line', data: [200, 250, 300, 500, 400, 300], color: '#94a3b8' }
    ]
  };
}
