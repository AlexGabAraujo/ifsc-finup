import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DetailCategoriaCardResponse } from '../../../../core/services/telaCategoria.service';

@Component({
  selector: 'app-card-categoria-model',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card-categoria-model.html',
})
export class CardCategoriaModel {
  @Input({ required: true }) categoria!: DetailCategoriaCardResponse;
  @Output() adicionarTransacao = new EventEmitter<number>();

  @Output() excluir = new EventEmitter<DetailCategoriaCardResponse>();

 onExcluir(): void {
  this.excluir.emit(this.categoria);
}

  formatarMoeda(v: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(v ?? 0);
  }

  abrirModalTransacao(): void {
    this.adicionarTransacao.emit(this.categoria.id);
  }



}