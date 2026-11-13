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
import { PluggyService, ItemBancario } from '../../../core/services/pluggy.service';

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
  @Output() abrirSincronizar = new EventEmitter<string>();

  private pluggyService = inject(PluggyService);
  private cdr = inject(ChangeDetectorRef);

  itens: ItemBancario[] = [];
  carregando = true;
  sincronizando: string | null = null;
  desconectando: string | null = null;

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

  sincronizar(item: ItemBancario): void {
    this.sincronizando = item.id;
    this.cdr.markForCheck();
    this.pluggyService.registrarItem(item.pluggyItemId).subscribe({
      next: () => {
        this.sincronizando = null;
        this.cdr.markForCheck();
      },
      error: () => {
        this.sincronizando = null;
        this.cdr.markForCheck();
      },
    });
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

  reconectar(item: ItemBancario): void {
    this.abrirSincronizar.emit(item.pluggyItemId);
  }
}
