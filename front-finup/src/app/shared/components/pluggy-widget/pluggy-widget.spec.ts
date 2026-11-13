import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import type { PluggyConnectProps } from 'pluggy-connect-sdk';
import { vi } from 'vitest';

import { PluggyWidget } from './pluggy-widget';
import {
  INTERVALO_CONSULTA_MS,
  PluggyService,
  StatusItem,
} from '../../../core/services/pluggy.service';
import { environment } from '../../../../environments/environment';

const API = environment.apiUrl;
const ITEM = { id: 'item-123', connector: { name: 'Banco Teste' }, status: 'UPDATED', executionStatus: 'SUCCESS' };
const URL_STATUS = `${API}/api/pluggy/itens/${ITEM.id}/status`;
const URL_TOKEN_UPDATE = `${API}/api/connect-token/update`;
const URL_SYNC = `${API}/api/pluggy/sync`;

function status(parcial: Partial<StatusItem> = {}): StatusItem {
  return {
    status: 'UPDATED',
    executionStatus: 'SUCCESS',
    ultimaAtualizacao: '2026-11-13T03:34:18.633Z',
    atualizacaoManual: true,
    ...parcial,
  };
}

describe('PluggyWidget', () => {
  let fixture: ComponentFixture<PluggyWidget>;
  let component: PluggyWidget;
  let http: HttpTestingController;
  let props: PluggyConnectProps | undefined;
  let sdk: { init: ReturnType<typeof vi.fn>; destroy: ReturnType<typeof vi.fn> };

  function criar(inputs: Partial<PluggyWidget>): void {
    fixture = TestBed.createComponent(PluggyWidget);
    component = fixture.componentInstance;
    Object.assign(component, { clientUserId: '7', nomeInstituicao: 'Banco Teste', ...inputs });
    fixture.detectChanges();
  }

  /** Item parado e atualizável: consulta o status, pede o token e abre o widget. */
  function abrirEmModoAtualizacao(): void {
    criar({ pluggyItemId: ITEM.id });
    http.expectOne(URL_STATUS).flush(status());
    const req = http.expectOne(URL_TOKEN_UPDATE);
    expect(req.request.body).toEqual({ itemId: ITEM.id });
    req.flush({ accessToken: 'tok' });
    props!.onOpen?.();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PluggyWidget],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);

    props = undefined;
    sdk = { init: vi.fn().mockResolvedValue(undefined), destroy: vi.fn().mockResolvedValue(undefined) };
    vi.spyOn(PluggyWidget.prototype as any, 'criarPluggyConnect').mockImplementation((p: unknown) => {
      props = p as PluggyConnectProps;
      return sdk;
    });
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  // ── fluxo normal ───────────────────────────────────────────────────────────

  it('refresh: abre o widget em modo updateItem com token gerado para o item', () => {
    abrirEmModoAtualizacao();

    expect(props!.connectToken).toBe('tok');
    expect(props!.updateItem).toBe(ITEM.id);
    expect(props!.forceAskForCredentials).toBe(false);
    expect(sdk.init).toHaveBeenCalled();
    expect(component.widgetAberto()).toBe(true);
    // nada é importado antes de a Pluggy terminar a atualização
    http.expectNone(URL_SYNC);
  });

  it('reconectar: força o formulário de credenciais', () => {
    criar({ pluggyItemId: ITEM.id, forcarCredenciais: true });
    http.expectOne(URL_STATUS).flush(status());
    http.expectOne(URL_TOKEN_UPDATE).flush({ accessToken: 'tok' });

    expect(props!.updateItem).toBe(ITEM.id);
    expect(props!.forceAskForCredentials).toBe(true);
  });

  it('conexão nova: não consulta status, usa o token comum e não envia updateItem', () => {
    criar({ pluggyItemId: null });
    http.expectOne(`${API}/api/connect-token`).flush({ accessToken: 'tok' });

    expect(props!.updateItem).toBeUndefined();
  });

  it('onSuccess: importa as transações, avisa as telas e mostra sucesso', () => {
    const recarregar = vi.fn();
    TestBed.inject(PluggyService).transacoesAtualizadas$.subscribe(recarregar);
    abrirEmModoAtualizacao();

    props!.onSuccess?.({ item: ITEM as any });

    const sync = http.expectOne(URL_SYNC);
    expect(sync.request.body).toEqual({ pluggyItemId: ITEM.id });
    expect(component.fase()).toBe('importando');
    expect(TestBed.inject(PluggyService).importando().has(ITEM.id)).toBe(true);

    props!.onClose?.(); // usuário fecha o widget com a importação em andamento
    expect(component.widgetAberto()).toBe(false);

    sync.flush(null);
    expect(component.fase()).toBe('sucesso');
    expect(recarregar).toHaveBeenCalledTimes(1);
    expect(TestBed.inject(PluggyService).importando().has(ITEM.id)).toBe(false);
  });

  it('falha na importação: "tentar novamente" só reimporta, sem reabrir o widget', () => {
    abrirEmModoAtualizacao();
    props!.onSuccess?.({ item: ITEM as any });
    props!.onClose?.();

    http.expectOne(URL_SYNC).flush('erro', { status: 504, statusText: 'Gateway Timeout' });
    expect(component.fase()).toBe('erro');
    expect(component.erroNaImportacao()).toBe(true);
    expect(TestBed.inject(PluggyService).falhas().has(ITEM.id)).toBe(true);

    component.tentarNovamente();

    http.expectNone(URL_TOKEN_UPDATE);
    http.expectOne(URL_SYNC).flush(null);
    expect(component.fase()).toBe('sucesso');
    expect(sdk.init).toHaveBeenCalledTimes(1);
    expect(TestBed.inject(PluggyService).falhas().has(ITEM.id)).toBe(false);
  });

  it('erro da Pluggy + fechar: mostra o motivo traduzido e não importa nada', () => {
    abrirEmModoAtualizacao();

    props!.onError?.({ message: 'Invalid credentials', data: { item: { ...ITEM, executionStatus: 'INVALID_CREDENTIALS' } as any } });
    expect(component.fase()).toBe('widget'); // o widget continua aberto para nova tentativa
    props!.onClose?.();

    expect(component.fase()).toBe('erro');
    expect(component.erro()).toContain('Reconectar');
    expect(component.erroNaImportacao()).toBe(false);
    http.expectNone(URL_SYNC);
  });

  it('falha ao gerar o connect token: mostra erro e permite tentar de novo', () => {
    criar({ pluggyItemId: ITEM.id });
    http.expectOne(URL_STATUS).flush(status());
    http.expectOne(URL_TOKEN_UPDATE).flush('x', { status: 500, statusText: 'Erro' });
    expect(component.fase()).toBe('erro');

    component.tentarNovamente();

    http.expectOne(URL_STATUS).flush(status());
    http.expectOne(URL_TOKEN_UPDATE).flush({ accessToken: 'tok2' });
    expect(props!.connectToken).toBe('tok2');
  });

  it('sem conseguir consultar o status: segue pelo widget normalmente', () => {
    criar({ pluggyItemId: ITEM.id });
    http.expectOne(URL_STATUS).flush('x', { status: 500, statusText: 'Erro' });
    http.expectOne(URL_TOKEN_UPDATE).flush({ accessToken: 'tok' });

    expect(props!.updateItem).toBe(ITEM.id);
  });

  // ── Meu Pluggy: a Pluggy não aceita atualização manual (PATCH 400) ─────────

  it('Meu Pluggy: não abre o widget, só importa o que a Pluggy já tem e explica', () => {
    criar({ pluggyItemId: ITEM.id, nomeInstituicao: 'MeuPluggy' });
    http.expectOne(URL_STATUS).flush(status({ atualizacaoManual: false }));

    http.expectNone(URL_TOKEN_UPDATE);
    expect(props).toBeUndefined();
    expect(component.fase()).toBe('importando');

    http.expectOne(URL_SYNC).flush(null);
    expect(component.fase()).toBe('sucesso');
    expect(component.aviso()).toContain('Meu Pluggy');
    expect(component.aviso()).toContain('Última atualização');
    expect(component.instituicao()).toBe('MeuPluggy');
  });

  it('Meu Pluggy + reconectar: abre o widget para autorizar de novo', () => {
    criar({ pluggyItemId: ITEM.id, forcarCredenciais: true });
    http.expectOne(URL_STATUS).flush(status({ atualizacaoManual: false }));

    http.expectOne(URL_TOKEN_UPDATE).flush({ accessToken: 'tok' });
    expect(props!.forceAskForCredentials).toBe(true);
  });

  // ── atualização já em andamento na Pluggy (PATCH daria ITEM_ALREADY_UPDATING) ──

  it('item já atualizando: não abre o widget, espera terminar e importa', () => {
    vi.useFakeTimers();
    criar({ pluggyItemId: ITEM.id });
    http.expectOne(URL_STATUS).flush(status({ status: 'UPDATING', executionStatus: 'LOGIN_IN_PROGRESS' }));

    expect(component.fase()).toBe('aguardando');
    expect(props).toBeUndefined();
    http.expectNone(URL_TOKEN_UPDATE);
    expect(TestBed.inject(PluggyService).importando().has(ITEM.id)).toBe(true);

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    http.expectOne(URL_STATUS).flush(status({ status: 'UPDATING', executionStatus: 'TRANSACTIONS_IN_PROGRESS' }));
    http.expectNone(URL_SYNC);

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    http.expectOne(URL_STATUS).flush(status());

    http.expectOne(URL_SYNC).flush(null);
    expect(component.fase()).toBe('sucesso');

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS * 3); // parou de consultar
    http.expectNone(URL_STATUS);
  });

  it('item já atualizando que termina em erro: mostra o motivo e não importa', () => {
    vi.useFakeTimers();
    criar({ pluggyItemId: ITEM.id });
    http.expectOne(URL_STATUS).flush(status({ status: 'UPDATING' }));

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    http.expectOne(URL_STATUS).flush(status({ status: 'LOGIN_ERROR', executionStatus: 'INVALID_CREDENTIALS' }));

    http.expectNone(URL_SYNC);
    expect(component.fase()).toBe('erro');
    expect(component.erro()).toContain('Reconectar');
    expect(component.erroNaImportacao()).toBe(false);
    expect(TestBed.inject(PluggyService).falhas().get(ITEM.id)).toContain('Reconectar');
  });

  it('uma consulta de status que falha no meio da espera não derruba o acompanhamento', () => {
    vi.useFakeTimers();
    criar({ pluggyItemId: ITEM.id });
    http.expectOne(URL_STATUS).flush(status({ status: 'UPDATING' }));

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    http.expectOne(URL_STATUS).flush('x', { status: 502, statusText: 'Bad Gateway' });
    expect(component.fase()).toBe('aguardando');

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    http.expectOne(URL_STATUS).flush(status());
    http.expectOne(URL_SYNC).flush(null);
    expect(component.fase()).toBe('sucesso');
  });

  // ── fechar o widget no meio ────────────────────────────────────────────────

  it('fechar o widget com a atualização rodando: continua acompanhando e importa no fim', () => {
    vi.useFakeTimers();
    const fechar = vi.fn();
    abrirEmModoAtualizacao();
    component.fechar.subscribe(fechar);

    props!.onClose?.(); // fechou enquanto a Pluggy ainda atualizava
    http.expectOne(URL_STATUS).flush(status({ status: 'UPDATING' }));

    expect(fechar).not.toHaveBeenCalled();
    expect(component.fase()).toBe('aguardando');

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    http.expectOne(URL_STATUS).flush(status());
    http.expectOne(URL_SYNC).flush(null);
    expect(component.fase()).toBe('sucesso');
  });

  it('fechar nosso modal durante a espera: o serviço termina a importação sozinho', () => {
    vi.useFakeTimers();
    const recarregar = vi.fn();
    TestBed.inject(PluggyService).transacoesAtualizadas$.subscribe(recarregar);
    criar({ pluggyItemId: ITEM.id });
    http.expectOne(URL_STATUS).flush(status({ status: 'UPDATING' }));

    component.fecharModal();
    fixture.destroy();

    vi.advanceTimersByTime(INTERVALO_CONSULTA_MS);
    http.expectOne(URL_STATUS).flush(status());
    http.expectOne(URL_SYNC).flush(null);

    expect(recarregar).toHaveBeenCalledTimes(1);
    expect(TestBed.inject(PluggyService).importando().has(ITEM.id)).toBe(false);
  });

  it('fechar o widget sem nada em andamento: emite fechar uma única vez', () => {
    const fechar = vi.fn();
    abrirEmModoAtualizacao();
    component.fechar.subscribe(fechar);

    component.fecharWidget(); // nosso botão flutuante
    props!.onClose?.(); // e o SDK ainda avisa o fechamento
    http.expectOne(URL_STATUS).flush(status());

    expect(sdk.destroy).toHaveBeenCalled();
    expect(fechar).toHaveBeenCalledTimes(1);
    http.expectNone(URL_SYNC);
  });
});
