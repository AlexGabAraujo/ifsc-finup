import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  inject,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ItemBancario,
  PedidoAtualizacaoItem,
  PluggyService,
} from '../../../core/services/pluggy.service';

@Component({
  selector: 'app-banco-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './banco-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BancoModal implements OnInit {
  @Input() clientUserId!: string;
  @Output() fechar = new EventEmitter<void>();
  @Output() abrirConectar = new EventEmitter<void>();
  /** Pede a abertura do Pluggy Connect em modo de atualização do item. */
  @Output() abrirSincronizar = new EventEmitter<PedidoAtualizacaoItem>();

  private pluggyService = inject(PluggyService);
  private cdr = inject(ChangeDetectorRef);

  itens: ItemBancario[] = [];
  carregando = true;
  desconectando: string | null = null;

  /** Itens cuja importação de transações ainda está rodando em segundo plano. */
  readonly importando = this.pluggyService.importando;
  /** Última falha de sincronização de cada item, para quem fechou o modal antes do fim. */
  readonly falhas = this.pluggyService.falhas;

  ngOnInit(): void {
    this.carregarItens();
  }

  carregarItens(): void {
    this.carregando = true;
    this.cdr.markForCheck();
    this.pluggyService.listarItens().subscribe({
      next: (itens) => {
        this.itens = itens;
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.carregando = false;
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Refresh: manda a Pluggy buscar os dados novos no banco (widget em modo updateItem)
   * e, quando ela terminar, o widget importa as transações. Chamar só o /sync do back
   * não traz nada novo — ele apenas relê o que a Pluggy já tinha coletado.
   */
  sincronizar(item: ItemBancario): void {
    this.abrirSincronizar.emit(this.pedido(item, false));
  }

  /** Reconectar: mesma atualização, mas pedindo as credenciais do banco de novo. */
  reconectar(item: ItemBancario): void {
    this.abrirSincronizar.emit(this.pedido(item, true));
  }

  private pedido(item: ItemBancario, forcarCredenciais: boolean): PedidoAtualizacaoItem {
    return {
      pluggyItemId: item.pluggyItemId,
      nomeInstituicao: item.nomeInstituicao || 'Banco',
      forcarCredenciais,
    };
  }

  desconectar(item: ItemBancario): void {
    this.desconectando = item.id;
    this.cdr.markForCheck();
    this.pluggyService.desconectarItem(item.id).subscribe({
      next: () => {
        this.itens = this.itens.filter(i => i.id !== item.id);
        this.desconectando = null;
        this.cdr.markForCheck();
      },
      error: () => {
        this.desconectando = null;
        this.cdr.markForCheck();
      },
    });
  }
}
