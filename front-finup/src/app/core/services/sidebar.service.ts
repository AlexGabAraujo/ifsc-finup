import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';

@Injectable({
  providedIn: 'root',
})
export class SidebarService {

 private menuFechado = new BehaviorSubject<boolean>(false);
  menuFechado$ = this.menuFechado.asObservable();

  toggleMenu() {
    this.menuFechado.next(!this.menuFechado.value);
  }

  get isFechado() {
    return this.menuFechado.value;
  }
}
