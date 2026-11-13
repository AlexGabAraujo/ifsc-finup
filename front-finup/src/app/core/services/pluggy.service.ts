import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Observable, Subject, throwError, timer } from 'rxjs';
import {
  catchError,
  exhaustMap,
  finalize,
  last,
  map,
  shareReplay,
  switchMap,
  takeWhile,
  tap,
  timeout,
} from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface ConnectTokenResponse {
  accessToken: string;
}

export interface PluggyItem {
  id: string;
  connector?: { name?: string };
  /** UPDATED | UPDATING | LOGIN_ERROR | OUTDATED | WAITING_USER_INPUT */
  status?: string;
  /** SUCCESS | PARTIAL_SUCCESS | INVALID_CREDENTIALS | SITE_NOT_AVAILABLE | ... */
  executionStatus?: string;
}

export interface PluggyItemSuccessPayload {
  item: PluggyItem;
}

export interface ItemBancario {
  id: string;
  pluggyItemId: string;
  nomeInstituicao: string;
}

/** Pedido de abertura do widget em modo de atualização de um item já conectado. */
export interface PedidoAtualizacaoItem {
  pluggyItemId: string;
  nomeInstituicao: string;
  /** true = pede as credenciais de novo mesmo que o item esteja conectado (reconectar). */
  forcarCredenciais: boolean;
}

/** Estado do item na Pluggy (GET /api/pluggy/itens/{id}/status). */
export interface StatusItem {
  status: string | null;
  executionStatus: string | null;
  /** ISO-8601 da última coleta bem-sucedida na Pluggy. */
  ultimaAtualizacao: string | null;
  /**
   * false para conexões do Meu Pluggy: são um espelho da conexão original e a Pluggy
   * não deixa atualizá-las pelo widget/API (o PATCH responde 400).
   */
  atualizacaoManual: boolean;
}

/** Erro com mensagem pronta para mostrar ao usuário. */
export class ErroPluggy extends Error {}

const MENSAGENS_ERRO_PLUGGY: Record<string, string> = {
  INVALID_CREDENTIALS:
    'As credenciais do banco não são mais válidas. Use "Reconectar" para informá-las novamente.',
  INVALID_CREDENTIALS_MFA: 'O código de verificação informado é inválido.',
  USER_INPUT_TIMEOUT: 'O tempo para informar o código de verificação esgotou.',
  ALREADY_LOGGED_IN:
    'Há outra sessão aberta nessa conta. Saia do app/site do banco e tente novamente.',
  SITE_NOT_AVAILABLE: 'A instituição está indisponível no momento. Tente novamente mais tarde.',
  CONNECTION_ERROR: 'Não foi possível falar com a instituição. Tente novamente mais tarde.',
  ACCOUNT_LOCKED: 'A conta está bloqueada no banco. Regularize o acesso e tente novamente.',
  ACCOUNT_NEEDS_ACTION:
    'O banco exige uma ação sua no app/site antes de liberar os dados. Resolva e tente novamente.',
  ACCOUNT_CREDENTIALS_RESET:
    'As credenciais foram redefinidas no banco. Use "Reconectar" para informá-las novamente.',
  USER_AUTHORIZATION_NOT_GRANTED: 'A autorização no banco não foi concedida.',
  USER_AUTHORIZATION_REVOKED:
    'A autorização foi revogada no banco. Use "Reconectar" para autorizar novamente.',
};

/** Traduz o executionStatus de erro da Pluggy; `null` se não for um erro conhecido. */
export function mensagemErroPluggy(executionStatus: string | null | undefined): string | null {
  return MENSAGENS_ERRO_PLUGGY[executionStatus ?? ''] ?? null;
}

/** Status em que a Pluggy ainda está trabalhando no item — abrir o widget agora daria 400. */
const STATUS_EM_ANDAMENTO = new Set(['UPDATING', 'MERGING', 'WAITING_USER_ACTION']);

export function atualizacaoEmAndamento(status: StatusItem): boolean {
  return STATUS_EM_ANDAMENTO.has(status.status ?? '');
}

/** De quanto em quanto tempo perguntamos à Pluggy se a atualização terminou. */
export const INTERVALO_CONSULTA_MS = 3_000;
/** Depois disso desistimos de esperar (o login no banco sozinho pode levar até 5 min). */
export const LIMITE_ESPERA_MS = 10 * 60_000;
const MAX_FALHAS_SEGUIDAS = 5;

@Injectable({ providedIn: 'root' })
export class PluggyService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  private emAndamento = new Map<string, Observable<void>>();
  private itensImportando = signal<ReadonlySet<string>>(new Set());
  private falhasPorItem = signal<ReadonlyMap<string, string>>(new Map());
  private transacoesAtualizadas = new Subject<void>();

  /** Ids (Pluggy) dos itens com atualização/importação em andamento. */
  readonly importando = this.itensImportando.asReadonly();

  /** Última falha de sincronização de cada item (para quem fechou o modal antes do fim). */
  readonly falhas = this.falhasPorItem.asReadonly();

  /** Emite sempre que uma importação termina — as telas usam para recarregar os dados. */
  readonly transacoesAtualizadas$ = this.transacoesAtualizadas.asObservable();

  gerarConnectToken(clientUserId: string): Observable<ConnectTokenResponse> {
    return this.http.post<ConnectTokenResponse>(`${this.apiUrl}/api/connect-token`, { clientUserId });
  }

  gerarConnectTokenUpdate(itemId: string): Observable<ConnectTokenResponse> {
    return this.http.post<ConnectTokenResponse>(`${this.apiUrl}/api/connect-token/update`, { itemId });
  }

  consultarStatusItem(pluggyItemId: string): Observable<StatusItem> {
    return this.http.get<StatusItem>(`${this.apiUrl}/api/pluggy/itens/${pluggyItemId}/status`);
  }

  /**
   * Importa para o FinUp as transações que a Pluggy já coletou do item.
   *
   * Deve ser chamado depois que a Pluggy terminou de atualizar o item (onSuccess do
   * widget) — sozinho ele NÃO busca dados novos no banco.
   *
   * A requisição pertence ao serviço, não a quem chamou: continua mesmo que o modal
   * seja fechado, e chamadas repetidas para o mesmo item reaproveitam a que já está
   * em andamento em vez de disparar outra.
   */
  importarTransacoes(pluggyItemId: string): Observable<void> {
    return this.executar(pluggyItemId, () => this.chamarImportacao(pluggyItemId));
  }

  /**
   * Para um item que a Pluggy ainda está atualizando (ex.: o usuário fechou o widget no
   * meio): espera a atualização terminar e então importa. Também roda no serviço, então
   * sobrevive ao fechamento do modal.
   */
  aguardarAtualizacaoEImportar(pluggyItemId: string): Observable<void> {
    return this.executar(pluggyItemId, () =>
      this.aguardarFimDaAtualizacao(pluggyItemId).pipe(
        switchMap((status) => {
          if (status.status === 'UPDATED') return this.chamarImportacao(pluggyItemId);

          return throwError(
            () =>
              new ErroPluggy(
                status.status === 'WAITING_USER_INPUT'
                  ? 'O banco pediu um código de verificação. Clique em atualizar de novo para informá-lo.'
                  : (mensagemErroPluggy(status.executionStatus) ??
                    'O banco não concluiu a atualização. Tente novamente mais tarde.'),
              ),
          );
        }),
      ),
    );
  }

  listarItens(): Observable<ItemBancario[]> {
    return this.http.get<ItemBancario[]>(`${this.apiUrl}/api/pluggy/itens`);
  }

  desconectarItem(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/pluggy/itens/${id}`);
  }

  private chamarImportacao(pluggyItemId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/api/pluggy/sync`, { pluggyItemId });
  }

  /** Consulta o status até a Pluggy sair de "em andamento" e devolve o status final. */
  private aguardarFimDaAtualizacao(pluggyItemId: string): Observable<StatusItem> {
    let falhasSeguidas = 0;

    return timer(INTERVALO_CONSULTA_MS, INTERVALO_CONSULTA_MS).pipe(
      exhaustMap(() =>
        this.consultarStatusItem(pluggyItemId).pipe(
          tap(() => (falhasSeguidas = 0)),
          catchError(() => {
            // Uma consulta que falha não derruba a espera; várias seguidas, sim.
            if (++falhasSeguidas < MAX_FALHAS_SEGUIDAS) return EMPTY;
            return throwError(
              () => new ErroPluggy('Não foi possível acompanhar a atualização do banco. Tente novamente.'),
            );
          }),
        ),
      ),
      takeWhile((status) => atualizacaoEmAndamento(status), true),
      last(),
      timeout({
        first: LIMITE_ESPERA_MS,
        with: () =>
          throwError(
            () =>
              new ErroPluggy(
                'O banco está demorando mais que o esperado para atualizar. Tente novamente mais tarde.',
              ),
          ),
      }),
    );
  }

  /**
   * Roda uma sincronização por item de cada vez, por conta do serviço: marca o item como
   * "importando", avisa as telas quando termina e guarda a falha se der errado.
   */
  private executar(pluggyItemId: string, fabrica: () => Observable<void>): Observable<void> {
    const jaRodando = this.emAndamento.get(pluggyItemId);
    if (jaRodando) return jaRodando;

    this.marcarImportando(pluggyItemId, true);
    this.registrarFalha(pluggyItemId, null);

    const execucao$ = fabrica().pipe(
      map(() => undefined),
      tap(() => this.transacoesAtualizadas.next()),
      catchError((err: unknown) => {
        this.registrarFalha(
          pluggyItemId,
          err instanceof ErroPluggy
            ? err.message
            : 'Não foi possível importar as transações. Tente atualizar novamente.',
        );
        return throwError(() => err);
      }),
      finalize(() => {
        this.emAndamento.delete(pluggyItemId);
        this.marcarImportando(pluggyItemId, false);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.emAndamento.set(pluggyItemId, execucao$);
    execucao$.subscribe({ error: () => {} });

    return execucao$;
  }

  private marcarImportando(pluggyItemId: string, ativo: boolean): void {
    this.itensImportando.update((atual) => {
      const novo = new Set(atual);
      if (ativo) novo.add(pluggyItemId);
      else novo.delete(pluggyItemId);
      return novo;
    });
  }

  private registrarFalha(pluggyItemId: string, mensagem: string | null): void {
    this.falhasPorItem.update((atual) => {
      const novo = new Map(atual);
      if (mensagem) novo.set(pluggyItemId, mensagem);
      else novo.delete(pluggyItemId);
      return novo;
    });
  }
}
