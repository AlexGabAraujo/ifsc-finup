import { Component, EventEmitter, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import {TelaCategoriaService, CreateCategoriaRequest} from '../../../../core/services/telaCategoria.service';
//      ↑↑↑↑ quatro pontos

// Validador: exige UMA das duas (classePrincipal OU subClasse, nunca ambas, nunca nenhuma)
export function categoriaExclusivaValidator(group: AbstractControl): ValidationErrors | null {
  const classeId = group.get('classePrincipalId')?.value;
  const subId = group.get('subClasseId')?.value;
  const ambos = classeId != null && subId != null;
  const nenhum = classeId == null && subId == null;
  return ambos || nenhum ? { categoriaInvalida: true } : null;
}

@Component({
  selector: 'app-categoria-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './categoria-form-modal.html',
})
export class CategoriaFormModal implements OnInit {
  @Output() salvo = new EventEmitter<void>();
  @Output() cancelado = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private categoriaService = inject(TelaCategoriaService);

  loading = signal(false);
  erroApi = signal('');
  classesPrincipais = signal<{ id: number; nome: string }[]>([]);
  subClasses = signal<{ id: number; nome: string }[]>([]);

  form = this.fb.group(
    {
      orcamento: [null as number | null, [Validators.required, Validators.min(0.01)]],
      classePrincipalId: [null as number | null],
      subClasseId: [null as number | null],
    },
    { validators: categoriaExclusivaValidator }
  );

  ngOnInit(): void {
    this.categoriaService.getClassesPrincipais().subscribe({
      next: (lista) => this.classesPrincipais.set(lista),
      error: (err) => console.error('Erro classes principais:', err),
    });

    this.categoriaService.getSubClasses().subscribe({
      next: (lista) => this.subClasses.set(lista),
      error: (err) => console.error('Erro subclasses:', err),
    });

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

  onSubmit(): void {
    this.erroApi.set('');

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;
    const dados: CreateCategoriaRequest = {
      orcamento: v.orcamento!,
      classePrincipalId: v.classePrincipalId ?? null,
      subClasseId: v.subClasseId ?? null,
    };

    this.loading.set(true);
    this.categoriaService.criar(dados).subscribe({
      next: () => {
        this.loading.set(false);
        this.salvo.emit();
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 400) {
          this.erroApi.set(err.error?.message ?? 'Dados inválidos.');
        } else {
          this.erroApi.set('Erro ao criar categoria. Tente novamente.');
        }
      },
    });
  }
}