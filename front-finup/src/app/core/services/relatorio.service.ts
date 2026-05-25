import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GraficoLinha, GraficoPizza } from './dashboard.service';
import { FiltroTransacao, InsightsResponse, TransacaoPageResponse } from '../../shared/models/transacao.models';
import { PeriodoRelatorio } from '../../features/relatorio/relatorio.utils';

// --- Tipos de filtro do relatório ---

export type TipoRelatorio = 'TODOS' | 'CREDITO' | 'DEBITO';

export interface FiltroRelatorio {
  periodo: PeriodoRelatorio;
  tipo: TipoRelatorio;
}

export interface RelatorioResumo {
  totalReceitas: number;
  totalDespesas: number;
  saldo: number;
  totalTransacoes: number;
}

export interface RelatorioGraficoBarras {
  meses: string[];
  receitas: number[];
  despesas: number[];
}

export interface RelatorioPizza {
  nome: string;
  valor: number;
}

export interface TopCategoriasMes {
  categoria: string;
  meses: string[];
  valores: number[];
}

// --- Serviço ---

@Injectable({ providedIn: 'root' })
export class RelatorioService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;
  private dashUrl = `${this.apiUrl}/api/dashboard`;
  private relatorioUrl = `${this.apiUrl}/api/relatorio`;

  private getHeaders() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` }),
    };
  }

  // --- Métodos legados (dashboard) ---

  carregarGraficoBarras(): Observable<GraficoLinha> {
    return this.http.get<GraficoLinha>(`${this.dashUrl}/graficoLinha`, this.getHeaders());
  }

  carregarGraficoPizza(): Observable<GraficoPizza[]> {
    return this.http.get<GraficoPizza[]>(`${this.dashUrl}/graficoPizza`, this.getHeaders());
  }

  carregarInsights(mes?: number, ano?: number): Observable<InsightsResponse> {
    let params = new HttpParams();
    if (mes != null) params = params.set('mes', mes);
    if (ano != null) params = params.set('ano', ano);
    return this.http.get<InsightsResponse>(`${this.apiUrl}/transacao/insights`, { params });
  }

  carregarTransacoes(filtro: FiltroTransacao, page: number): Observable<TransacaoPageResponse> {
    let params = new HttpParams().set('page', page);
    if (filtro.mes != null) params = params.set('mes', filtro.mes);
    if (filtro.ano != null) params = params.set('ano', filtro.ano);
    if (filtro.categoriaId != null) params = params.set('categoriaId', filtro.categoriaId);
    if (filtro.categoriaType != null) params = params.set('categoriaType', filtro.categoriaType);
    return this.http.get<TransacaoPageResponse>(`${this.apiUrl}/transacao`, { ...this.getHeaders(), params });
  }

  // --- Novos métodos (/api/relatorio) ---

  carregarResumo(periodo: PeriodoRelatorio): Observable<RelatorioResumo> {
    const params = new HttpParams().set('periodo', periodo);
    return this.http.get<RelatorioResumo>(`${this.relatorioUrl}/resumo`, { ...this.getHeaders(), params });
  }

  carregarGraficoBarrasRelatorio(periodo: PeriodoRelatorio): Observable<RelatorioGraficoBarras> {
    const params = new HttpParams().set('periodo', periodo);
    return this.http.get<RelatorioGraficoBarras>(`${this.relatorioUrl}/graficoBarras`, { ...this.getHeaders(), params });
  }

  carregarGraficoPizzaRelatorio(periodo: PeriodoRelatorio): Observable<RelatorioPizza[]> {
    const params = new HttpParams().set('periodo', periodo);
    return this.http.get<RelatorioPizza[]>(`${this.relatorioUrl}/graficoPizza`, { ...this.getHeaders(), params });
  }

  carregarTopCategoriasPorMes(periodo: PeriodoRelatorio): Observable<TopCategoriasMes[]> {
    const params = new HttpParams().set('periodo', periodo);
    return this.http.get<TopCategoriasMes[]>(`${this.relatorioUrl}/topCategoriasPorMes`, { ...this.getHeaders(), params });
  }

  carregarTransacoesComPeriodo(periodo: PeriodoRelatorio, page: number): Observable<TransacaoPageResponse> {
    const params = new HttpParams().set('periodo', periodo).set('page', page);
    return this.http.get<TransacaoPageResponse>(`${this.relatorioUrl}/transacoes`, { ...this.getHeaders(), params });
  }

  periodoParaParams(periodo: PeriodoRelatorio): { mes?: number; ano?: number } {
    const agora = new Date();
    const mesAtual = agora.getMonth() + 1;
    const anoAtual = agora.getFullYear();

    switch (periodo) {
      case 'MES_ATUAL':
        return { mes: mesAtual, ano: anoAtual };
      case 'MES_ANTERIOR': {
        const mesAnterior = mesAtual === 1 ? 12 : mesAtual - 1;
        const anoMesAnterior = mesAtual === 1 ? anoAtual - 1 : anoAtual;
        return { mes: mesAnterior, ano: anoMesAnterior };
      }
      case 'ULTIMOS_3_MESES':
        return { ano: anoAtual };
      case 'ULTIMOS_6_MESES':
        return { ano: anoAtual };
      case 'ANO_ATUAL':
        return { ano: anoAtual };
      case 'ANO':
        return {};
      default:
        return {};
    }
  }
}
