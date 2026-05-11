import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoriaFormModal } from './categoria-form-modal';

describe('CategoriaFormModal', () => {
  let component: CategoriaFormModal;
  let fixture: ComponentFixture<CategoriaFormModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoriaFormModal]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CategoriaFormModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
