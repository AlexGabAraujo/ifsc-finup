import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TransacaoService } from '../../../core/services/transacao.service';
import { CategoriaService } from '../../../core/services/categoria.service';
import {
  CategoriaOption,
  CreateTransacaoRequest,
  DetailTransacaoResponse,
} from '../../../shared/models/transacao.models';

export function categoriaExclusivaValidator(group: AbstractControl): ValidationErrors | null {
  const classeId = group.get('classePrincipalId')?.value;
  const subId = group.get('subClasseId')?.value;
  const ambos = classeId != null && subId != null;
  const nenhum = classeId == null && subId == null;
  return ambos || nenhum ? { categoriaInvalida: true } : null;
}

@Component({
  selector: 'app-transacao-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transacao-form-modal.html',
})
export class TransacaoFormModal implements OnInit {
  @Input() transacao: DetailTransacaoResponse | null = null;
  @Output() salvo = new EventEmitter<void>();
  @Output() cancelado = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private transacaoService = inject(TransacaoService);
  private categoriaService = inject(CategoriaService);

  loading = false;
  erroApi = '';
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
    },
    { validators: categoriaExclusivaValidator }
  );

  ngOnInit(): void {
    this.categoriaService.getClassesPrincipais().subscribe({
      next: (lista) => {
        this.classesPrincipais = lista.map((c) => ({ id: c.id, nome: c.nome, type: 'CLASSE_PRINCIPAL' as const }));
      },
    });

    this.categoriaService.getSubClasses().subscribe({
      next: (lista) => {
        this.subClasses = lista.map((s) => ({ id: s.id, nome: s.nome, type: 'SUBCLASSE' as const }));
      },
    });

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

    this.form.get('classePrincipalId')?.valueChanges.subscribe((val) => {
      if (val != null) {
        this.form.get('subClasseId')?.setValue(null, { emitEvent: false });
      }
    });

    this.form.get('subClasseId')?.valueChanges.subscribe((val) => {
      if (val != null) {
        this.form.get('classePrincipalId')?.setValue(null, { emitEvent: false });
      }
    });
  }

  get modoEdicao(): boolean {
    return this.transacao != null;
  }

  onSubmit(): void {
    this.erroApi = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;
    const dados: CreateTransacaoRequest = {
      valor: v.valor!,
      tipoGasto: v.tipoGasto as any,
      tipoPagamento: v.tipoPagamento as any,
      dataTransacao: v.dataTransacao!,
      classePrincipalId: v.classePrincipalId ?? null,
      subClasseId: v.subClasseId ?? null,
      cnpjId: v.cnpjId ?? null,
    };

    this.loading = true;

    const req$ = this.modoEdicao
      ? this.transacaoService.atualizar(this.transacao!.id, dados)
      : this.transacaoService.criar(dados);

    req$.subscribe({
      next: () => {
        this.loading = false;
        this.salvo.emit();
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 403) {
          this.erroApi = 'Você não tem permissão para realizar esta ação.';
        } else if (err.status === 400) {
          this.erroApi = err.error?.message ?? 'Dados inválidos. Verifique os campos.';
        } else {
          this.erroApi = 'Ocorreu um erro. Tente novamente.';
        }
      },
    });
  }
}
