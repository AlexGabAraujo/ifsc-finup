import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { TelaCategoriaService } from './telaCategoria.service';

export interface InfoDashboard {
  ReceitaTotal: number;
  metaEconomica: number;
  saldoAtual: number;
  totalDespesa: number;
  valorMetaAtual: number;
  estatisticaSaldoAtual: number;
  estatisticaDespesasAtual: number;
  estatisticaReceitaAtual: number;
  porcenMetaEco: number;
}

export interface GraficoLinha {
  categoriasX: string[];
  seriesReceita: number[];
  seriesDespesa: number[];
}

export interface GraficoPizza {
  nome: string;
  valor: number;
}

export interface TransacoesRecentes {
  categoria: string;
  valor: number;
  data: string;
  tipoGasto: 'CREDITO' | 'DEBITO';
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private apiUrl = 'http://localhost:8080/api/dashboard';

  constructor(private http: HttpClient, private TelaCategoriaService: TelaCategoriaService) {}

  getHeaders() {
    const token = localStorage.getItem('token'); // Use a chave exata que o Java retorna
    return {
        headers: new HttpHeaders({
            'Authorization': `Bearer ${token}`
        })
    };
}

  getInfoDashboard(): Observable<InfoDashboard> {
    return this.http.get<InfoDashboard>(`${this.apiUrl}/informacoesGerais`, this.getHeaders());
  }

  getGraficoLinha(): Observable<GraficoLinha> {
    return this.http.get<GraficoLinha>(`${this.apiUrl}/graficoLinha`, this.getHeaders());
  }

  getGraficoPizza(): Observable<GraficoPizza[]> {
    return this.http.get<GraficoPizza[]>(`${this.apiUrl}/graficoPizza`, this.getHeaders());
  }

  getTransacoesRecentes(): Observable<TransacoesRecentes[]> {
    return this.http.get<TransacoesRecentes[]>(`${this.apiUrl}/Transacoes`, this.getHeaders());
  }

  getUltimasTransacoes(categoriaId: number | null) {
    let params = new HttpParams();
    if (categoriaId !== null) params = params.set('categoriaId', categoriaId);
    return this.http.get<any[]>(`${this.apiUrl}/Transacoes`, { params });
  }

  // Reaproveita o endpoint existente, extrai só id + nome
  listarCategorias() {
    return this.TelaCategoriaService.getOrcamentoTotal('MES_ATUAL');
  }
}

