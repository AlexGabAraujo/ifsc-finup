import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CardCateg } from './card-categ';

describe('CardCateg', () => {
  let component: CardCateg;
  let fixture: ComponentFixture<CardCateg>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardCateg]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CardCateg);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
