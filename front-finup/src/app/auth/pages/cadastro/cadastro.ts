import { Component, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AutenticacaoService } from '../../../core/services/autenticacao.service';
import { CreateAccountRequest } from '../../../shared/models/usuario.models';

@Component({
  selector: 'app-cadastro',
  standalone: true,
  templateUrl: './cadastro.html',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
})
export class CadastroComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AutenticacaoService);
  private router = inject(Router);

  loading = false;
  errorMsg = '';

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(6)]],
    username: ['', [Validators.required]],
    nome: ['', [Validators.required]],
    cpf: ['', [Validators.required]],
    telefone: ['', [Validators.required]],
    dataNascimento: [''],
  });

  submit() {
    this.errorMsg = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: CreateAccountRequest = {
      email: this.form.value.email ?? '',
      senha: this.form.value.senha ?? '',
      username: this.form.value.username ?? '',
      nome: this.form.value.nome ?? '',
      cpf: this.form.value.cpf ?? '',
      telefone: this.form.value.telefone ?? '',
      dataNascimento: this.form.value.dataNascimento || null,
    };


    this.loading = true;

    this.auth.cadastrarUsuario(payload).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/login');
      },
      error: () => {
        this.loading = false;
        this.errorMsg = 'Não foi possível realizar o cadastro. Verifique os dados informados.';
      },
    });
  }
}