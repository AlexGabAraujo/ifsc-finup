import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalMetaEco } from './modal-meta-eco';

describe('ModalMetaEco', () => {
  let component: ModalMetaEco;
  let fixture: ComponentFixture<ModalMetaEco>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalMetaEco]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ModalMetaEco);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
