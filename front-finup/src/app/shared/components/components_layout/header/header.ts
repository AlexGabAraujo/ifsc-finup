import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { filter } from 'rxjs/operators';
import { RouterModule } from '@angular/router';
import { AutenticacaoService } from '../../../../core/services/autenticacao.service';
import { ProfileService } from '../../../../core/services/profile.service';
import { SidebarService } from '../../../../core/services/sidebar.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header implements OnInit {

  pageTitle: string = '';
  userName: string = '';
  userEmail: string = '';
  userInitials: string = '';
  notificationCount: number = 0;
  menu_open: boolean = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private authService: AutenticacaoService,
    private profileService: ProfileService,
    public sidebarService: SidebarService
  ) { }


  ngOnInit() {
    // Atualiza o título conforme a rota ativa
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        let rota_atual = this.route;
        while (rota_atual.firstChild) {
          rota_atual = rota_atual.firstChild;
        }
        this.pageTitle = rota_atual.snapshot.data['title'];
      });

    // Carrega os dados reais do usuário autenticado
    this.profileService.getProfile().subscribe({
      next: (data) => {
        this.userName = data.nome;
        this.userEmail = data.email;
        this.userInitials = this.getInitials(data.nome);
      }
    });
  }

  private getInitials(nome: string): string {
    const parts = nome.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  icone_notificacao(): void {
    console.log('Abrir painel de notificações');
  }

  icone_user(): void {
    this.menu_open = !this.menu_open;
  }

  logout(): void {
    this.authService.logout();
  }

  toggleMenu() {
    console.log('Botão do Header clicado!');
    this.sidebarService.toggleMenu();
  }
}
