import { Component } from '@angular/core';
import { HighchartsChartComponent } from 'highcharts-angular';
import { CardInfo } from "../../shared/components/cards/card-info/card-info";
import { CardChart } from '../../shared/components/cards/card-chart/card-chart';
import { CardLine } from '../../shared/components/cards/card-line/card-line';
import Highcharts from 'highcharts';


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CardInfo, CardChart, CardLine, HighchartsChartComponent],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})

export class Dashboard {

  exibirSaldo: boolean = true;

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
  tooltip: { shared: true, useHTML: true, pointFormat: '<span style="color:{series.color}">●</span> {series.name}: <b>R$ {point.y:.2f}</b><br/>'},
  plotOptions: {line: {marker: {enabled: true,symbol: 'circle',}}},
  credits: { enabled: false },
  series: [
    { name: 'Receitas', type: 'line', data: [4000, 4500, 4200, 5000, 4800, 5500], color: '#10b981' },
    { name: 'Despesas', type: 'line', data: [3500, 3800, 3800, 4100, 3900, 4200], color: '#f43f5e' }
  ] as any,
};

  pieChartOptions: Highcharts.Options = {
  chart: {
    type: 'pie',
    backgroundColor: 'transparent'
  },
  title: { text: '' },
  
  legend: {
    enabled: true,
    layout: 'vertical',    
    align: 'left',       
    verticalAlign: 'middle',
    itemStyle: {
      color: '#374151', 
      fontSize: '12px',
      fontWeight: '600'
    },
    itemMarginBottom: 10,  
    symbolRadius: 4        
  },

  plotOptions: {
    pie: {
      innerSize: '60%',
      borderRadius: 0,
      dataLabels: { enabled: false },
      showInLegend: true,
      states: {
        hover: { brightness: 0.1 }
      }
    }
  },
  tooltip: {
    enabled: true,
    useHTML: true,
    headerFormat: '',
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    shadow: true,
    pointFormat: '<b>{point.name}</b>: {point.percentage:.1f}%'
  },
  series: [{
    type: 'pie',
    data: [
      { name: 'Suporte', y: 21.3, color: '#6366f1' },
      { name: 'Dev', y: 18.7, color: '#10b981' },
      { name: 'Vendas', y: 20.2, color: '#f59e0b' },
      { name: 'Marketing', y: 14.2, color: '#f43f5e' },
      { name: 'Outros', y: 25.6, color: '#94a3b8' }
    ]
  }] as any
};

  
  iconeSaldo() {
    this.exibirSaldo = !this.exibirSaldo;
  }
}


