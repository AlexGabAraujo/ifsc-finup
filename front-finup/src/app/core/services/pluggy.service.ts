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

export interface ItemBancario {
  id: string;
  pluggyItemId: string;
  nomeInstituicao: string;
}

@Injectable({ providedIn: 'root' })
export class PluggyService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  gerarConnectToken(clientUserId: string): Observable<ConnectTokenResponse> {
    return this.http.post<ConnectTokenResponse>(`${this.apiUrl}/api/connect-token`, { clientUserId });
  }

  gerarConnectTokenUpdate(itemId: string): Observable<ConnectTokenResponse> {
    return this.http.post<ConnectTokenResponse>(`${this.apiUrl}/api/connect-token/update`, { itemId });
  }

  registrarItem(pluggyItemId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/api/pluggy/sync`, { pluggyItemId });
  }

  listarItens(): Observable<ItemBancario[]> {
    return this.http.get<ItemBancario[]>(`${this.apiUrl}/api/pluggy/itens`);
  }

  desconectarItem(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/pluggy/itens/${id}`);
  }
}
