import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type PeriodoFiltro =
  | 'MES_ATUAL'
  | 'MES_ANTERIOR'
  | 'ULTIMOS_3_MESES'
  | 'ULTIMOS_6_MESES'
  | 'ANO_ATUAL';

export interface DetailCategoriaCardResponse {
  id: number;
  nome: string;
  descricao: string | null;
  orcamento: number;
  gasto: number;
  disponivel: number;
  percentualGasto: number;
  gastoPeriodoAnterior: number;
  variacaoPercentual: number;
  quantidadeTransacoes: number;
}

export interface DetailCategoriaOrcamentoResponse {
  orcamentoTotal: number;
  gastoTotal: number;
  disponivel: number;
  percentualUso: number;
  categorias: DetailCategoriaCardResponse[];   // ← repare no nome "categorias"
}

export interface DatasetCategoria {
  categoriaId: number;
  nome: string;
  data: number[];   // ← atenção: o backend usa "data", não "serie"
}

export interface DetailGraficoCategoriaResponse {
  labels: string[];
  datasets: DatasetCategoria[];
}

export interface DetailCategoriaResponse {
  id: number;
  orcamento: number;
  pessoaFisicaId: number;
  subClasseId?: number | null;
  classePrincipalId?: number | null;
}

export interface CreateCategoriaRequest {
  orcamento: number;
  classePrincipalId: number | null;
  subClasseId: number | null;
}



@Injectable({
  providedIn: 'root',
})
export class TelaCategoriaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getOrcamentoTotal(periodo: PeriodoFiltro = 'MES_ATUAL'): Observable<DetailCategoriaOrcamentoResponse> {
    const params = new HttpParams().set('periodo', periodo);
    return this.http.get<DetailCategoriaOrcamentoResponse>(`${this.apiUrl}/categoria/orcamento-total`, { params });
  }

  getGraficoLinha(periodo: PeriodoFiltro = 'ULTIMOS_6_MESES'): Observable<DetailGraficoCategoriaResponse> {
    const params = new HttpParams().set('periodo', periodo);
    return this.http.get<DetailGraficoCategoriaResponse>(`${this.apiUrl}/categoria/grafico-linha`, { params });
  }

  buscarPorId(id: number): Observable<DetailCategoriaResponse> {
    return this.http.get<DetailCategoriaResponse>(
      `${this.apiUrl}/categoria/${id}`
    );
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/categoria/${id}`);
  }

  getClassesPrincipais(): Observable<{ id: number; nome: string }[]> {
    return this.http.get<{ id: number; nome: string }[]>(`${this.apiUrl}/classePrincipal`);
  }
  getSubClasses(): Observable<{ id: number; nome: string }[]> {
    return this.http.get<{ id: number; nome: string }[]>(`${this.apiUrl}/subClasse`);
  }

  criar(dados: CreateCategoriaRequest): Observable<DetailCategoriaResponse> {
    return this.http.post<DetailCategoriaResponse>(`${this.apiUrl}/categoria`, dados);
  }

}
