import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateTransacaoRequest,
  DetailTransacaoResponse,
  FiltroTransacao,
  InsightsResponse,
  TransacaoPageResponse,
  UpdateTransacaoRequest,
} from '../../shared/models/transacao.models';




@Injectable({ providedIn: 'root' })
export class TransacaoService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getInsights(mes?: number | null, ano?: number | null): Observable<InsightsResponse> {
    let params = new HttpParams();
    if (mes != null) params = params.set('mes', mes);
    if (ano != null) params = params.set('ano', ano);
    return this.http.get<InsightsResponse>(`${this.apiUrl}/transacao/insights`, { params });
  }

  listar(filtro: FiltroTransacao, page: number): Observable<TransacaoPageResponse> {
    let params = new HttpParams().set('page', page);
    if (filtro.mes != null) params = params.set('mes', filtro.mes);
    if (filtro.ano != null) params = params.set('ano', filtro.ano);
    if (filtro.categoriaId != null) params = params.set('categoriaId', filtro.categoriaId);
    if (filtro.categoriaType != null) params = params.set('categoriaType', filtro.categoriaType);
    return this.http.get<TransacaoPageResponse>(`${this.apiUrl}/transacao`, { params });
  }

  criar(dados: CreateTransacaoRequest): Observable<DetailTransacaoResponse> {
    return this.http.post<DetailTransacaoResponse>(`${this.apiUrl}/transacao`, dados);
  }

  atualizar(id: number, dados: UpdateTransacaoRequest): Observable<DetailTransacaoResponse> {
    return this.http.put<DetailTransacaoResponse>(`${this.apiUrl}/transacao/${id}`, dados);
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/transacao/${id}`);
  }

  getClassesPrincipais(): Observable<{ id: number; nome: string }[]> {
    return this.http.get<{ id: number; nome: string }[]>(`${this.apiUrl}/transacao/classes-principais`);
  }

  getSubClassesPorClasse(classePrincipalId: number): Observable<{ id: number; nome: string }[]> {
    const params = new HttpParams().set('classePrincipalId', classePrincipalId);
    return this.http.get<{ id: number; nome: string }[]>(`${this.apiUrl}/transacao/subclasses`, { params });
  }
}
