import { Component, inject } from '@angular/core';
import { FormBuilder, Validators, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AutenticacaoService } from '../../../core/services/autenticacao.service';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
})

export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AutenticacaoService);
  private router = inject(Router);

  loading = false;
  errorMsg = '';

  form = this.fb.group({
    login: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(3)]],
  });

  ngOnInit() {
    this.auth.logout();
  }

  submit() {
    this.errorMsg = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { login, password } = this.form.getRawValue() as any;

    this.loading = true;
    this.auth.login(login, password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/dashboard');
      },
      error: () => {
        this.loading = false;
        this.errorMsg = 'Não foi possível entrar. Verifique suas credenciais.';
      },
    });
  }
}