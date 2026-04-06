import { Component, ChangeDetectorRef, inject, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ProfileService } from '../../core/services/profile.service';
import { UpdateAccountRequest } from '../../shared/models/profile.models';
import { ButtonComponent } from '../../shared/components/button/button';

export function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const nova = group.get('novaSenha')?.value;
  const confirmar = group.get('confirmarSenha')?.value;
  return nova && confirmar && nova !== confirmar ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ButtonComponent],
  templateUrl: './profile.html',
})
export class ProfilePage implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private cdr = inject(ChangeDetectorRef);

  // Estado de loading
  loadingGeneral = false;
  loadingEmail = false;
  loadingPassword = false;

  // Estado de sucesso
  successGeneral = '';
  successEmail = '';
  successPassword = '';

  // Estado de erro
  errorGeneral = '';
  errorEmail = '';
  errorPassword = '';

  // Dados somente leitura
  cpf = '';
  currentEmail = '';

  // GeneralForm: nome, username, telefone, dataNascimento
  generalForm = this.fb.group({
    nome: ['', [Validators.required]],
    username: ['', [Validators.required, Validators.minLength(6)]],
    telefone: ['', [Validators.required, Validators.pattern(/^(?:\(?\d{2}\)?\s?)?9?\d{4}-?\d{4}$/)]],
    dataNascimento: [''],
  });

  // DangerEmailForm: novoEmail
  dangerEmailForm = this.fb.group({
    novoEmail: ['', [Validators.required, Validators.email]],
  });

  // DangerPasswordForm: novaSenha, confirmarSenha + validador cruzado
  dangerPasswordForm = this.fb.group(
    {
      novaSenha: ['', [Validators.required, Validators.pattern(/^(?=.*[^A-Za-z0-9]).{6,}$/)]],
      confirmarSenha: ['', [Validators.required]],
    },
    { validators: passwordMatchValidator }
  );

  ngOnInit(): void {
    this.loadingGeneral = true;
    this.profileService.getProfile().subscribe({
      next: (data) => {
        this.loadingGeneral = false;
        this.cpf = data.cpf;
        this.currentEmail = data.email;
        this.generalForm.patchValue({
          nome: data.nome,
          username: data.username,
          telefone: data.telefone,
          dataNascimento: data.dataNascimento,
        });
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingGeneral = false;
        this.errorGeneral = 'Não foi possível carregar os dados do perfil.';
        this.cdr.markForCheck();
      },
    });
  }

  onSaveGeneral(): void {
    this.errorGeneral = '';
    this.successGeneral = '';

    if (this.generalForm.invalid) {
      this.generalForm.markAllAsTouched();
      return;
    }

    const payload: UpdateAccountRequest = {
      email: this.currentEmail,
      senha: null,
      username: this.generalForm.value.username ?? '',
      nome: this.generalForm.value.nome ?? '',
      telefone: this.generalForm.value.telefone ?? '',
      dataNascimento: this.generalForm.value.dataNascimento ?? '',
    };

    this.loadingGeneral = true;
    this.profileService.updateProfile(payload).subscribe({
      next: () => {
        this.loadingGeneral = false;
        this.successGeneral = 'Dados atualizados com sucesso!';
      },
      error: () => {
        this.loadingGeneral = false;
        this.errorGeneral = 'Não foi possível atualizar os dados. Tente novamente.';
      },
    });
  }

  onSaveEmail(): void {
    this.errorEmail = '';
    this.successEmail = '';

    if (this.dangerEmailForm.invalid) {
      this.dangerEmailForm.markAllAsTouched();
      return;
    }

    const payload: UpdateAccountRequest = {
      email: this.dangerEmailForm.value.novoEmail ?? '',
      senha: null,
      username: this.generalForm.value.username ?? '',
      nome: this.generalForm.value.nome ?? '',
      telefone: this.generalForm.value.telefone ?? '',
      dataNascimento: this.generalForm.value.dataNascimento ?? '',
    };

    this.loadingEmail = true;
    this.profileService.updateProfile(payload).subscribe({
      next: () => {
        this.loadingEmail = false;
        this.currentEmail = payload.email;
        this.dangerEmailForm.reset();
        this.successEmail = 'E-mail atualizado com sucesso!';
      },
      error: () => {
        this.loadingEmail = false;
        this.errorEmail = 'Não foi possível atualizar o e-mail. Tente novamente.';
      },
    });
  }

  onSavePassword(): void {
    this.errorPassword = '';
    this.successPassword = '';

    if (this.dangerPasswordForm.invalid) {
      this.dangerPasswordForm.markAllAsTouched();
      return;
    }

    const payload: UpdateAccountRequest = {
      email: this.currentEmail,
      senha: this.dangerPasswordForm.value.novaSenha ?? '',
      username: this.generalForm.value.username ?? '',
      nome: this.generalForm.value.nome ?? '',
      telefone: this.generalForm.value.telefone ?? '',
      dataNascimento: this.generalForm.value.dataNascimento ?? '',
    };

    this.loadingPassword = true;
    this.profileService.updateProfile(payload).subscribe({
      next: () => {
        this.loadingPassword = false;
        this.dangerPasswordForm.reset();
        this.successPassword = 'Senha atualizada com sucesso!';
      },
      error: () => {
        this.loadingPassword = false;
        this.errorPassword = 'Não foi possível atualizar a senha. Tente novamente.';
      },
    });
  }
}
