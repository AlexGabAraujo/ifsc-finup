import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CardChart } from './card-chart';

describe('CardChart', () => {
  let component: CardChart;
  let fixture: ComponentFixture<CardChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardChart]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CardChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
