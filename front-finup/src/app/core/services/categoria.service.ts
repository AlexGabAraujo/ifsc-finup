import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getClassesPrincipais(): Observable<{ id: number; nome: string }[]> {
    return this.http.get<{ id: number; nome: string }[]>(`${this.apiUrl}/classePrincipal`);
  }

  getSubClasses(): Observable<{ id: number; nome: string }[]> {
    return this.http.get<{ id: number; nome: string }[]>(`${this.apiUrl}/subClasse`);
  }
}
