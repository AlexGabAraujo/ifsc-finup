import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DetailAccountResponse, UpdateAccountRequest } from '../../shared/models/profile.models';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<DetailAccountResponse> {
    return this.http.get<DetailAccountResponse>(`${this.apiUrl}/auth/account`);
  }

  updateProfile(payload: UpdateAccountRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/auth`, payload);
  }
}
