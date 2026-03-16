import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CardLine } from './card-line';

describe('CardLine', () => {
  let component: CardLine;
  let fixture: ComponentFixture<CardLine>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardLine]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CardLine);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
