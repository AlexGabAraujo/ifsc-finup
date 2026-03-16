import { Component } from '@angular/core';
import {RouterLink} from '@angular/router';
import { ButtonComponent } from '../../shared/components/button/button';

@Component({
  selector: 'app-not-found',
  imports: [ ButtonComponent, RouterLink ],
  templateUrl: './not-found.html'
})
export class NotFound {

}
