import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TransacaoService } from '../../../core/services/transacao.service';
import { TelaCategoriaService } from '../../../core/services/telaCategoria.service';
import {
  CategoriaOption,
  CreateTransacaoRequest,
  DetailTransacaoResponse,
} from '../../../shared/models/transacao.models';
import { AutenticacaoService } from '../../../core/services/autenticacao.service';
import { ChangeDetectorRef } from '@angular/core';
import { firstValueFrom } from 'rxjs/internal/firstValueFrom';

@Component({
  selector: 'app-transacao-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transacao-form-modal.html',
})
export class TransacaoFormModal implements OnInit {
  @Input() transacao: DetailTransacaoResponse | null = null;
  @Input() categoriaId: number | null = null;
  @Output() salvo = new EventEmitter<void>();
  @Output() cancelado = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private transacaoService = inject(TransacaoService);
  private categoriaService = inject(TelaCategoriaService);
  private authService = inject(AutenticacaoService);
  private cdr = inject(ChangeDetectorRef);

  loading = false;
  erroApi = '';
  subClasseDesabilitada = true;

  classesPrincipais: CategoriaOption[] = [];
  subClasses: CategoriaOption[] = [];

  form = this.fb.group(
    {
      valor: [null as number | null, [Validators.required, Validators.min(0.01)]],
      tipoGasto: ['', Validators.required],
      tipoPagamento: ['', Validators.required],
      dataTransacao: ['', Validators.required],
      classePrincipalId: [null as number | null],
      subClasseId: [null as number | null],
      cnpjId: [null as number | null],
    });

  async ngOnInit(): Promise<void> {
    try {
      const lista = await firstValueFrom(this.transacaoService.getClassesPrincipais());
      this.classesPrincipais = lista.map((c) => ({ id: c.id, nome: c.nome, type: 'CLASSE_PRINCIPAL' as const }));
      this.cdr.markForCheck();
    } catch (err) {
      console.log('Erro classes principais', err);
    }

    if (this.transacao) {
      this.form.patchValue({
        valor: this.transacao.valor,
        tipoGasto: this.transacao.tipoGasto,
        tipoPagamento: this.transacao.tipoPagamento,
        dataTransacao: this.transacao.dataTransacao,
        classePrincipalId: this.transacao.classePrincipalId,
        subClasseId: this.transacao.subClasseId,
        cnpjId: this.transacao.cnpjId,
      });
    }

    if (this.categoriaId !== null) {
      this.preencherCategoriaAutomatica(this.categoriaId);
      this.form.get('tipoGasto')?.setValue('DEBITO');
      this.form.get('tipoGasto')?.disable();
    }

    // Quando selecionar classe principal → carrega subclasses do usuário
    this.form.get('classePrincipalId')?.valueChanges.subscribe((val) => {
      this.form.get('subClasseId')?.setValue(null, { emitEvent: false });
      this.subClasses = [];
      this.subClasseDesabilitada = true;

      if (val !== null && val !== undefined) {
        this.transacaoService.getSubClassesPorClasse(Number(val)).subscribe({
          next: (lista) => {
            this.subClasses = lista.map((s) => ({ id: s.id, nome: s.nome, type: 'SUBCLASSE' as const }));
            this.subClasseDesabilitada = lista.length === 0;
            this.cdr.markForCheck();
          },
          error: (err) => console.log('Erro subclasses', err),
        });
      }
    });
  }

  preencherCategoriaAutomatica(categoriaId: number): void {
    this.categoriaService.buscarPorId(categoriaId).subscribe({
      next: (categoria) => {
        this.form.patchValue({
          classePrincipalId: categoria.classePrincipalId ?? null,
          subClasseId: categoria.subClasseId ?? null,
        }, { emitEvent: false }); 

        this.form.updateValueAndValidity();
        this.cdr.markForCheck();
      },
      error: () => {
        this.erroApi = 'Erro ao carregar dados da categoria.';
      }
    });
  }

  get modoEdicao(): boolean {
    return this.transacao !== null;
  }

  onSubmit(): void {
    this.erroApi = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.authService.getAccount().subscribe({
      next: (account) => {
        const v = this.form.getRawValue();
        const dados: CreateTransacaoRequest = {
          valor: v.valor!,
          pessoaFisicaId: account.id_pessoa, // ← campo correto
          tipoGasto: v.tipoGasto as any,
          tipoPagamento: v.tipoPagamento as any,
          dataTransacao: v.dataTransacao!,
          classePrincipalId: v.subClasseId ? null : v.classePrincipalId ?? null,
          subClasseId: v.subClasseId ?? null,
          cnpjId: v.cnpjId ?? null,
          categoriaId: this.categoriaId,
        };

        const req$ = this.modoEdicao
          ? this.transacaoService.atualizar(this.transacao!.id, dados)
          : this.transacaoService.criar(dados);

        req$.subscribe({
          next: () => { this.loading = false; this.salvo.emit(); },
          error: (err) => {
            this.loading = false;
            if (err.status === 403) {
              this.erroApi = 'Você não tem permissão para realizar esta ação.';
            } else if (err.status === 400) {
              this.erroApi = err.error?.message ?? 'Dados inválidos. Verifique os campos.';
            } else {
              this.erroApi = 'Ocorreu um erro. Tente novamente.';
            }
            this.cdr.markForCheck();
          },
        });
      },
      error: () => {
        this.loading = false;
        this.erroApi = 'Erro ao identificar usuário. Faça login novamente.';
      }
    });
  }

}