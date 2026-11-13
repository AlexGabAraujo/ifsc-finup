import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SidebarService } from '../../../../core/services/sidebar.service';
import { AutenticacaoService } from '../../../../core/services/autenticacao.service';
import { PluggyWidget } from '../../pluggy-widget/pluggy-widget';
import { BancoModal } from '../../banco-modal/banco-modal';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule, PluggyWidget, BancoModal],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar implements OnInit {
  menuFechado = false;
  clientUserId = '';

  bancoModalAberto = false;
  pluggyWidgetAberto = false;
  pluggyItemIdUpdate: string | null = null;

  private sidebarService = inject(SidebarService);
  private autenticacaoService = inject(AutenticacaoService);

  ngOnInit() {
    this.sidebarService.menuFechado$.subscribe((estado) => {
      this.menuFechado = estado;
    });

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

  abrirBancoModal(): void {
    this.bancoModalAberto = true;
  }

  fecharBancoModal(): void {
    this.bancoModalAberto = false;
  }

  abrirPluggyWidget(): void {
    this.bancoModalAberto = false;
    this.pluggyItemIdUpdate = null;
    this.pluggyWidgetAberto = true;
  }

  abrirPluggyWidgetUpdate(pluggyItemId: string): void {
    this.bancoModalAberto = false;
    this.pluggyItemIdUpdate = pluggyItemId;
    this.pluggyWidgetAberto = true;
  }

  fecharPluggyWidget(): void {
    this.pluggyWidgetAberto = false;
    this.pluggyItemIdUpdate = null;
  }
}
