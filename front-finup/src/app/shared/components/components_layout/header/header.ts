import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { filter } from 'rxjs/operators';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  
  pageTitle: string = 'Dashboard';
  userName: string = 'João da Silva';
  userEmail: string = 'joao@email.com';
  userInitials: string = 'JD';
  notificationCount: number = 0;
  menu_open: boolean = false;


  constructor(
    private router: Router,
    private route: ActivatedRoute,
  ) { }

  ngOnInit() {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        let rota_atual = this.route;
        while (rota_atual.firstChild) {
          rota_atual = rota_atual.firstChild;
        }
        this.pageTitle = rota_atual.snapshot.data['title'];
      });
  }

  icone_notificacao(): void {
    console.log('Abrir painel de notificações');
  }

  icone_user(): void {
    this.menu_open = !this.menu_open;
  }

  logout(): void {
    console.log('Logout');
  }

  @HostListener('document:click', ['$event'])
  menu_click(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.relative')) {
      this.menu_open = false;
    }
  }
}
