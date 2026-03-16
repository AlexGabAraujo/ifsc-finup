import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateAccountRequest } from '../../shared/models/usuario.models';

interface AuthResponse {
  'jwt-token': string;
}

const KEY = 'token';

@Injectable({
  providedIn: 'root',
})
export class AutenticacaoService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
  ) {}

  login(username: any, senha: any): Observable<HttpResponse<AuthResponse>>  {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/auth/login`,
      { username, senha },
      { observe: 'response'}
    ).pipe(
      tap((response) => {
        const authToken = response.body? response.body['jwt-token'] : '';
        this.salvarToken(authToken);
      })
    );
  }

  cadastrarUsuario(usuario: CreateAccountRequest): Observable<HttpResponse<AuthResponse>> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/auth/register`,
      usuario,
      { observe: 'response' }
    ).pipe(
      tap((response) => {
        const authToken = response.body ? response.body['jwt-token'] : '';
        this.salvarToken(authToken);
      })
    );
  }

  salvarToken(token: string) {
    localStorage.setItem(KEY, token);
  }

  logout(){
    localStorage.clear;
  }

}
