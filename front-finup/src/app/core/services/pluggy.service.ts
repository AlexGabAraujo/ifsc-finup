import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ConnectTokenResponse {
  accessToken: string;
}

export interface PluggyItemSuccessPayload {
  item: {
    id: string;
    connector: { name: string };
    status: string;
  };
}

@Injectable({ providedIn: 'root' })
export class PluggyService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  /**
   * Solicita ao backend um connectToken do Pluggy para o usuário autenticado.
   * O backend chama a API do Pluggy com as credenciais server-side.
   */
  gerarConnectToken(clientUserId: string): Observable<ConnectTokenResponse> {
    return this.http.post<ConnectTokenResponse>(`${this.apiUrl}/api/connect-token`, {
      clientUserId,
    });
  }

  /**
   * Notifica o backend para registrar e sincronizar o item Pluggy
   * após o usuário conectar a conta com sucesso.
   */
  registrarItem(pluggyItemId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/api/pluggy/sync`, { pluggyItemId });
  }
}
