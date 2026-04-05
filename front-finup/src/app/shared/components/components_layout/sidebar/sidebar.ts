import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { SidebarService } from '../../../../core/services/sidebar.service';


@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  menuFechado = false;

  constructor(private sidebarService: SidebarService) { }

  ngOnInit() {
    this.sidebarService.menuFechado$.subscribe((estado) => {
      console.log('Sidebar recebeu novo estado:', estado);
      this.menuFechado = estado;
    });
  }

  toggleMenu() {
    this.menuFechado = !this.menuFechado;
    this.sidebarService.toggleMenu();
  }
}
