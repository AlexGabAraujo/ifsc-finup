import { Component, EventEmitter, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TelaCategoriaService, CreateCategoriaRequest } from '../../../../core/services/telaCategoria.service';


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
  subClasseDesabilitada = true;

  form = this.fb.group({
    orcamento: [null as number | null, [Validators.required, Validators.min(0.01)]],
    classePrincipalId: [null as number | null, Validators.required],
    subClasseId: [{ value: null as number | null, disabled: true }],
  });

  ngOnInit(): void {
    this.categoriaService.getClassesPrincipais().subscribe({
      next: (lista) => this.classesPrincipais.set(lista),
      error: (err) => console.error('Erro classes principais:', err),
    });

    // Quando selecionar classe principal → carrega subclasses e limpa subclasse anterior
    this.form.get('classePrincipalId')?.valueChanges.subscribe((val) => {
      this.form.get('subClasseId')?.setValue(null, { emitEvent: false });
      this.subClasses.set([]);

      if (val != null) {
        this.subClasseDesabilitada = false;
        this.categoriaService.getSubClassesPorClasse(val).subscribe({
          next: (lista) => this.subClasses.set(lista),
          error: (err) => console.error('Erro subclasses:', err),
        });
      } else {
        this.subClasseDesabilitada = true; // ← desabilita
      }
    });
  }

  onSubmit(): void {
    this.erroApi.set('');

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const dados: CreateCategoriaRequest = {
      orcamento: v.orcamento!,
      classePrincipalId: v.subClasseId ? null : v.classePrincipalId ?? null,
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