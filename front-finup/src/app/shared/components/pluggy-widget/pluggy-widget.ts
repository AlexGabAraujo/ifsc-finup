import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  inject,
  Input,
  OnDestroy,
  OnInit,
  Output,
  signal,
} from '@angular/core';
import { PluggyConnect, type PluggyConnectProps } from 'pluggy-connect-sdk';
import {
  atualizacaoEmAndamento,
  ErroPluggy,
  mensagemErroPluggy,
  PluggyItem,
  PluggyItemSuccessPayload,
  PluggyService,
  StatusItem,
} from '../../../core/services/pluggy.service';

/**
 * verificando → (só atualização) perguntando à Pluggy em que estado o item está
 * token       → pedindo o connect token ao back
 * widget      → Pluggy Connect montando/aberto (conexão ou atualização do item na Pluggy)
 * aguardando  → a Pluggy ainda está atualizando o item sem o widget; importamos ao terminar
 * importando  → Pluggy terminou; back importando as transações para o FinUp
 * sucesso     → transações importadas
 * erro        → falha em qualquer etapa (ver `erroNaImportacao`)
 */
type Fase = 'verificando' | 'token' | 'widget' | 'aguardando' | 'importando' | 'sucesso' | 'erro';

const FORMATO_DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

@Component({
  selector: 'app-pluggy-widget',
  standalone: true,
  templateUrl: './pluggy-widget.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PluggyWidget implements OnInit, OnDestroy {
  @Input() clientUserId!: string;
  /** Se informado, o widget abre em modo de atualização desse item (botão de refresh). */
  @Input() pluggyItemId: string | null = null;
  /** No modo de atualização, força o formulário de credenciais (botão de reconectar). */
  @Input() forcarCredenciais = false;
  /** Nome do banco do item em atualização (em conexão nova, vem do próprio widget). */
  @Input() nomeInstituicao = '';

  @Output() fechar = new EventEmitter<void>();
  @Output() contaConectada = new EventEmitter<PluggyItemSuccessPayload>();

  private pluggyService = inject(PluggyService);

  readonly fase = signal<Fase>('token');
  /** true enquanto o iframe do Pluggy Connect está na tela, por cima do nosso modal. */
  readonly widgetAberto = signal(false);
  readonly erro = signal<string | null>(null);
  /** O erro aconteceu depois que a Pluggy já atualizou o item: basta reimportar. */
  readonly erroNaImportacao = signal(false);
  readonly aviso = signal<string | null>(null);
  readonly instituicao = signal('');

  private widgetInstance: PluggyConnect | null = null;
  /** Item devolvido pelo onSuccess — a partir daqui só falta importar. */
  private itemPronto: PluggyItem | null = null;
  /** Último erro reportado pelo widget; só é exibido se o usuário fechar sem sucesso. */
  private erroDoWidget: string | null = null;
  /** Evita tratar duas vezes o fechamento do mesmo widget (nosso botão + onClose do SDK). */
  private fechamentoTratado = false;
  private destruido = false;

  /** true = atualizando um item já conectado; false = conectando um banco novo. */
  get atualizando(): boolean {
    return !!this.pluggyItemId;
  }

  ngOnInit(): void {
    this.iniciar();
  }

  ngOnDestroy(): void {
    this.destruido = true;
    this.destruirWidget();
  }

  // ── Etapa 0: (só atualização) em que estado o item está na Pluggy? ─────────

  private iniciar(): void {
    this.destruirWidget();
    this.itemPronto = null;
    this.erroDoWidget = null;
    this.erro.set(null);
    this.erroNaImportacao.set(false);
    this.aviso.set(null);
    this.instituicao.set(this.nomeInstituicao || 'Instituição');

    const itemId = this.pluggyItemId;
    if (!itemId) {
      this.pedirToken();
      return;
    }

    // Abrir o widget de atualização às cegas falha com 400 (PATCH /items) quando a Pluggy
    // já está atualizando o item ou quando ele é do Meu Pluggy. Por isso perguntamos antes.
    this.fase.set('verificando');
    this.pluggyService.consultarStatusItem(itemId).subscribe({
      next: (status) => {
        if (this.destruido) return;

        if (atualizacaoEmAndamento(status)) {
          this.acompanharAtualizacao(itemId);
        } else if (!status.atualizacaoManual && !this.forcarCredenciais) {
          this.importarEspelho(itemId, status);
        } else {
          this.pedirToken();
        }
      },
      error: (err: unknown) => {
        // Sem o status seguimos pelo caminho normal: o widget lida com o que encontrar.
        console.warn('[PluggyWidget] Não foi possível consultar o status do item:', err);
        if (!this.destruido) this.pedirToken();
      },
    });
  }

  /**
   * Conexão do Meu Pluggy: é um espelho da conexão original, que o próprio Meu Pluggy
   * atualiza. Não há o que pedir à Pluggy — só importamos o que ela já tem.
   */
  private importarEspelho(itemId: string, status: StatusItem): void {
    const quando = status.ultimaAtualizacao
      ? ` Última atualização: ${FORMATO_DATA_HORA.format(new Date(status.ultimaAtualizacao))}.`
      : '';
    this.aviso.set(
      'Esta conexão vem do Meu Pluggy, que atualiza os dados por conta própria (a cada 24h). ' +
        'Para trazer lançamentos mais recentes, atualize a conta em meu.pluggy.ai e clique em atualizar aqui de novo.' +
        quando,
    );
    this.itemPronto = { id: itemId };
    this.importar();
  }

  /** A Pluggy está atualizando o item sem o widget: espera terminar e importa. */
  private acompanharAtualizacao(itemId: string): void {
    this.itemPronto = { id: itemId };
    this.erro.set(null);
    this.erroNaImportacao.set(false);
    this.fase.set('aguardando');

    this.pluggyService.aguardarAtualizacaoEImportar(itemId).subscribe({
      next: () => this.fase.set('sucesso'),
      error: (err: unknown) => this.aoFalharSincronizacao(err),
    });
  }

  // ── Etapa 1: connect token ────────────────────────────────────────────────

  private pedirToken(): void {
    this.fase.set('token');

    // Em modo de atualização o token precisa ser criado com o itemId (doc Pluggy:
    // connect-widget/updating-item), senão a Pluggy recusa o updateItem.
    const token$ = this.pluggyItemId
      ? this.pluggyService.gerarConnectTokenUpdate(this.pluggyItemId)
      : this.pluggyService.gerarConnectToken(this.clientUserId);

    token$.subscribe({
      next: ({ accessToken }) => {
        if (!this.destruido) this.abrirWidget(accessToken);
      },
      error: (err: unknown) => {
        console.error('[PluggyWidget] Erro ao gerar connectToken:', err);
        this.falhar(
          this.atualizando
            ? 'Não foi possível iniciar a atualização. Tente novamente.'
            : 'Não foi possível iniciar a conexão. Tente novamente.',
        );
      },
    });
  }

  // ── Etapa 2: Pluggy Connect (conexão / atualização do item) ───────────────

  private abrirWidget(connectToken: string): void {
    this.fase.set('widget');
    this.fechamentoTratado = false;

    const props: PluggyConnectProps = {
      connectToken,
      includeSandbox: true,
      language: 'pt',
      onOpen: () => this.widgetAberto.set(true),
      onSuccess: ({ item }) => this.aoConcluirNaPluggy(item as PluggyItem),
      onError: (error) => {
        // O próprio widget mostra o erro e deixa tentar de novo; só guardamos o motivo
        // para exibir caso o usuário feche sem conseguir.
        console.error('[PluggyWidget] Erro no widget:', error);
        const item = error?.data?.item as PluggyItem | undefined;
        this.erroDoWidget =
          mensagemErroPluggy(item?.executionStatus) ??
          error?.message ??
          'Ocorreu um erro ao falar com o banco.';
      },
      onClose: () => this.aoFecharWidget(),
    };

    if (this.pluggyItemId) {
      // Modo de atualização: se o banco não exigir nada do usuário, a Pluggy já
      // dispara a atualização sozinha; se exigir (senha/MFA), o widget pede.
      props.updateItem = this.pluggyItemId;
      props.forceAskForCredentials = this.forcarCredenciais;
    }

    try {
      this.widgetInstance = this.criarPluggyConnect(props);
      this.widgetInstance.init().catch((err: unknown) => this.falharAoAbrir(err));
    } catch (err) {
      this.falharAoAbrir(err);
    }
  }

  /** Ponto único de criação do SDK (substituído nos testes). */
  protected criarPluggyConnect(props: PluggyConnectProps): PluggyConnect {
    return new PluggyConnect(props);
  }

  private falharAoAbrir(err: unknown): void {
    console.error('[PluggyWidget] Erro ao abrir o widget:', err);
    this.fechamentoTratado = true;
    this.widgetAberto.set(false);
    this.falhar('Não foi possível abrir o widget do banco. Tente novamente.');
  }

  /** onSuccess: a Pluggy terminou de coletar os dados do item. */
  private aoConcluirNaPluggy(item: PluggyItem): void {
    if (!item?.id) {
      this.erroDoWidget = 'A Pluggy não retornou o item atualizado.';
      return;
    }

    this.itemPronto = item;
    this.erroDoWidget = null;
    this.instituicao.set(item.connector?.name ?? (this.nomeInstituicao || 'Instituição'));
    this.aviso.set(
      item.executionStatus === 'PARTIAL_SUCCESS'
        ? 'O banco não liberou todos os dados nesta atualização; alguns lançamentos podem ficar para a próxima.'
        : null,
    );
    this.contaConectada.emit({ item });

    // Começa a importar já, enquanto o widget ainda mostra a tela de sucesso dele.
    this.importar();
  }

  /** Usuário fechou o widget (pelo X dele, pelo "continuar" ou pelo nosso botão). */
  private aoFecharWidget(): void {
    if (this.fechamentoTratado) return;
    this.fechamentoTratado = true;
    this.widgetAberto.set(false);

    if (this.itemPronto) return; // nosso modal assume, mostrando importando/sucesso/erro

    if (this.erroDoWidget) {
      this.falhar(this.erroDoWidget);
    } else if (this.pluggyItemId) {
      this.retomarSeAindaAtualizando(this.pluggyItemId);
    } else {
      this.fechar.emit(); // desistiu sem erro: fecha tudo
    }
  }

  /**
   * Fechar o widget não cancela a atualização: ela continua na Pluggy. Se for o caso,
   * acompanhamos até o fim para não perder as transações (e para o próximo clique em
   * atualizar não bater em "item já está atualizando").
   */
  private retomarSeAindaAtualizando(itemId: string): void {
    this.fase.set('verificando');
    this.pluggyService.consultarStatusItem(itemId).subscribe({
      next: (status) => {
        if (this.destruido) return;
        if (atualizacaoEmAndamento(status)) {
          this.acompanharAtualizacao(itemId);
        } else {
          this.fechar.emit(); // nada em andamento: o usuário só desistiu
        }
      },
      error: () => this.fechar.emit(),
    });
  }

  // ── Etapa 3: importar as transações no FinUp ──────────────────────────────

  private importar(): void {
    const item = this.itemPronto;
    if (!item) return;

    this.erro.set(null);
    this.erroNaImportacao.set(false);
    this.fase.set('importando');

    this.pluggyService.importarTransacoes(item.id).subscribe({
      next: () => this.fase.set('sucesso'),
      error: (err: unknown) => this.aoFalharSincronizacao(err),
    });
  }

  private aoFalharSincronizacao(err: unknown): void {
    console.error('[PluggyWidget] Erro ao sincronizar o item:', err);

    if (err instanceof ErroPluggy) {
      this.falhar(err.message); // a Pluggy não concluiu: tentar de novo recomeça do status
      return;
    }

    // Falhou a chamada de importação: os dados já estão na Pluggy, basta reimportar.
    this.erroNaImportacao.set(true);
    this.falhar('Não conseguimos importar as transações do banco para o FinUp.');
  }

  private falhar(mensagem: string): void {
    this.erro.set(mensagem);
    this.fase.set('erro');
  }

  // ── Ações do template ─────────────────────────────────────────────────────

  /** Botão flutuante sobre o iframe do Pluggy. */
  fecharWidget(): void {
    this.destruirWidget();
    this.aoFecharWidget();
  }

  fecharModal(): void {
    this.destruirWidget();
    this.fechar.emit();
  }

  tentarNovamente(): void {
    if (this.erroNaImportacao()) {
      this.importar(); // a Pluggy já está atualizada: não precisa reabrir o widget
    } else {
      this.iniciar();
    }
  }

  private destruirWidget(): void {
    const instancia = this.widgetInstance;
    this.widgetInstance = null;
    if (!instancia) return;
    try {
      instancia.destroy().catch(() => {
        /* widget já fechado */
      });
    } catch {
      /* widget já fechado */
    }
  }
}
