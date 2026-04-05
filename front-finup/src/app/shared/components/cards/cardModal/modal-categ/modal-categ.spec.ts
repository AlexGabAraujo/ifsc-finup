import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalCateg } from './modal-categ';

describe('ModalCateg', () => {
  let component: ModalCateg;
  let fixture: ComponentFixture<ModalCateg>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalCateg]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ModalCateg);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
