import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalCadTrns } from './modal-cad-trns';

describe('ModalCadTrns', () => {
  let component: ModalCadTrns;
  let fixture: ComponentFixture<ModalCadTrns>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalCadTrns]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ModalCadTrns);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
