import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SidebarService } from '../../../../core/services/sidebar.service';
import { AutenticacaoService } from '../../../../core/services/autenticacao.service';
import { PluggyWidget } from '../../pluggy-widget/pluggy-widget';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule, PluggyWidget],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar implements OnInit {
  menuFechado = false;
  pluggyWidgetAberto = false;
  clientUserId = '';

  private sidebarService = inject(SidebarService);
  private autenticacaoService = inject(AutenticacaoService);

  ngOnInit() {
    this.sidebarService.menuFechado$.subscribe((estado) => {
      this.menuFechado = estado;
    });

    // Busca o ID da pessoa física para usar como clientUserId no Pluggy
    this.autenticacaoService.getAccount().subscribe({
      next: (conta: any) => {
        this.clientUserId = String(conta?.id_pessoa ?? conta?.id ?? '');
      },
      error: () => {},
    });
  }

  toggleMenu() {
    this.menuFechado = !this.menuFechado;
    this.sidebarService.toggleMenu();
  }

  abrirPluggyWidget(): void {
    this.pluggyWidgetAberto = true;
  }

  fecharPluggyWidget(): void {
    this.pluggyWidgetAberto = false;
  }
}
