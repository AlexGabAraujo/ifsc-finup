import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  inject,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { PluggyConnect } from 'pluggy-connect-sdk';
import { PluggyService, PluggyItemSuccessPayload } from '../../../core/services/pluggy.service';

@Component({
  selector: 'app-pluggy-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pluggy-widget.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PluggyWidget implements OnInit, OnDestroy {
  @Input() clientUserId!: string;
  @Output() fechar = new EventEmitter<void>();
  @Output() contaConectada = new EventEmitter<PluggyItemSuccessPayload>();

  private pluggyService = inject(PluggyService);
  private cdr = inject(ChangeDetectorRef);
  private ngZone = inject(NgZone);

  carregando = true;
  erro: string | null = null;
  conectado = false;
  widgetAberto = false;
  instituicaoConectada = '';

  // Tipado como any para evitar conflito com tipos internos do zoid
  private widgetInstance: any = null;

  ngOnInit(): void {
    this.iniciarWidget();
  }

  ngOnDestroy(): void {
    try { this.widgetInstance?.destroy(); } catch { /* silencioso */ }
  }

  private iniciarWidget(): void {
    this.carregando = true;
    this.erro = null;
    this.widgetAberto = false;
    this.cdr.markForCheck();

    this.pluggyService.gerarConnectToken(this.clientUserId).subscribe({
      next: ({ accessToken }) => {
        this.ngZone.run(() => {
          this.carregando = false;
          this.cdr.markForCheck();
          setTimeout(() => this.abrirPluggyConnect(accessToken), 50);
        });
      },
      error: (err: unknown) => {
        this.ngZone.run(() => {
          this.carregando = false;
          this.erro = 'Não foi possível iniciar a conexão. Tente novamente.';
          this.cdr.markForCheck();
          console.error('[PluggyWidget] Erro ao gerar connectToken:', err);
        });
      },
    });
  }

  private abrirPluggyConnect(connectToken: string): void {
    this.widgetInstance = new PluggyConnect({
      connectToken,
      includeSandbox: true, // inclui conectores de sandbox para testes

      onSuccess: (itemData) => {
        this.ngZone.run(() => {
          this.conectado = true;
          this.widgetAberto = false;
          this.instituicaoConectada = (itemData as any)?.item?.connector?.name ?? 'Instituição';
          this.contaConectada.emit(itemData as unknown as PluggyItemSuccessPayload);
          this.cdr.markForCheck();

          const itemId = (itemData as any)?.item?.id;
          if (itemId) {
            this.pluggyService.registrarItem(itemId).subscribe({
              error: (err: unknown) =>
                console.error('[PluggyWidget] Erro ao sincronizar item:', err),
            });
          }
        });
      },

      onError: (error) => {
        this.ngZone.run(() => {
          this.widgetAberto = false;
          this.erro = error?.message ?? 'Ocorreu um erro ao conectar a conta.';
          this.cdr.markForCheck();
          console.error('[PluggyWidget] Erro no widget:', error);
        });
      },

      onClose: () => {
        this.ngZone.run(() => {
          this.widgetAberto = false;
          if (!this.conectado) {
            this.fechar.emit();
          }
          this.cdr.markForCheck();
        });
      },

      onOpen: () => {
        this.ngZone.run(() => {
          this.widgetAberto = true;
          this.cdr.markForCheck();
        });
      },
    });

    this.widgetInstance.init();
  }

  fecharModal(): void {
    try { this.widgetInstance?.destroy(); } catch { /* silencioso */ }
    this.fechar.emit();
  }

  tentarNovamente(): void {
    try { this.widgetInstance?.destroy(); } catch { /* silencioso */ }
    this.widgetInstance = null;
    this.conectado = false;
    this.iniciarWidget();
  }
}
