import { Component, ChangeDetectorRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TransacaoService } from '../../core/services/transacao.service';
import { CategoriaService } from '../../core/services/categoria.service';
import { CardInfo } from '../../shared/components/cards/card-info/card-info';
import { TransacaoFormModal } from './transacao-form-modal/transacao-form-modal';
import {
  CategoriaOption,
  DetailTransacaoResponse,
  FiltroTransacao,
  InsightsResponse,
  TransacaoPageResponse,
} from '../../shared/models/transacao.models';

@Component({
  selector: 'app-transacoes',
  standalone: true,
  imports: [CommonModule, FormsModule, CardInfo, TransacaoFormModal],
  templateUrl: './transacoes.html',
})
export class Transacoes implements OnInit {
  private transacaoService = inject(TransacaoService);
  private categoriaService = inject(CategoriaService);
  private cdr = inject(ChangeDetectorRef);

  filtro: FiltroTransacao = { mes: null, ano: null, categoriaId: null, categoriaType: null };
  paginaAtual = 0;
  loading = false;
  insights: InsightsResponse | null = null;
  transacoes: TransacaoPageResponse | null = null;
  modalAberto = false;
  transacaoEditando: DetailTransacaoResponse | null = null;
  categorias: CategoriaOption[] = [];

  anos: number[] = [];

  meses = [
    { valor: 1, nome: 'Janeiro' }, { valor: 2, nome: 'Fevereiro' },
    { valor: 3, nome: 'Março' }, { valor: 4, nome: 'Abril' },
    { valor: 5, nome: 'Maio' }, { valor: 6, nome: 'Junho' },
    { valor: 7, nome: 'Julho' }, { valor: 8, nome: 'Agosto' },
    { valor: 9, nome: 'Setembro' }, { valor: 10, nome: 'Outubro' },
    { valor: 11, nome: 'Novembro' }, { valor: 12, nome: 'Dezembro' },
  ];

  ngOnInit(): void {
    // Gerar lista dos últimos 5 anos
    const anoAtual = new Date().getFullYear();
    this.anos = Array.from({ length: 5 }, (_, i) => anoAtual - i);

    // Carregar categorias combinando ClassePrincipal e SubClasse
    forkJoin({
      classes: this.categoriaService.getClassesPrincipais().pipe(catchError(() => of([]))),
      subs: this.categoriaService.getSubClasses().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ classes, subs }) => {
        this.categorias = [
          ...classes.map((c) => ({ id: c.id, nome: c.nome, type: 'CLASSE_PRINCIPAL' as const })),
          ...subs.map((s) => ({ id: s.id, nome: s.nome, type: 'SUBCLASSE' as const })),
        ];
        this.cdr.markForCheck();
      },
    });

    this.carregarDados();
  }

  carregarDados(): void {
    this.loading = true;
    forkJoin({
      insights: this.transacaoService.getInsights(this.filtro.mes, this.filtro.ano).pipe(catchError(() => of(null))),
      transacoes: this.transacaoService.listar(this.filtro, this.paginaAtual).pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ insights, transacoes }) => {
        this.insights = insights;
        this.transacoes = transacoes;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  onFiltroAlterado(): void {
    this.paginaAtual = 0;
    this.carregarDados();
  }

  onPaginaAlterada(p: number): void {
    this.paginaAtual = p;
    this.carregarDados();
  }

  abrirModal(t?: DetailTransacaoResponse): void {
    this.transacaoEditando = t ?? null;
    this.modalAberto = true;
  }

  fecharModal(): void {
    this.modalAberto = false;
    this.transacaoEditando = null;
  }

  onTransacaoSalva(): void {
    this.fecharModal();
    this.carregarDados();
  }

  confirmarExclusao(id: number): void {
    if (confirm('Deseja excluir esta transação?')) {
      this.transacaoService.deletar(id).subscribe({
        next: () => this.carregarDados(),
      });
    }
  }

  get mostrando(): string {
    if (!this.transacoes || this.transacoes.totalElements === 0) return 'Nenhuma transação';
    const inicio = this.paginaAtual * this.transacoes.pageSize + 1;
    const fim = Math.min(inicio + this.transacoes.pageSize - 1, this.transacoes.totalElements);
    return `Mostrando ${inicio}-${fim} de ${this.transacoes.totalElements} transações`;
  }

  formatarValor(v: number): string {
    return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  // Retorna o nome da categoria de uma transação
  nomeCategoria(t: DetailTransacaoResponse): string {
    if (t.classePrincipalId != null) {
      return this.categorias.find((c) => c.id === t.classePrincipalId && c.type === 'CLASSE_PRINCIPAL')?.nome ?? '—';
    }
    if (t.subClasseId != null) {
      return this.categorias.find((c) => c.id === t.subClasseId && c.type === 'SUBCLASSE')?.nome ?? '—';
    }
    return '—';
  }

  onCategoriaFiltroAlterado(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const val = select.value;
    if (!val) {
      this.filtro = { ...this.filtro, categoriaId: null, categoriaType: null };
    } else {
      const [type, id] = val.split(':');
      this.filtro = { ...this.filtro, categoriaId: Number(id), categoriaType: type as any };
    }
    this.onFiltroAlterado();
  }
}
