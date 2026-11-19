import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/api.config';
import { CategoryManager } from './category-manager';

describe('CategoryManager', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('creates a category after validating and trimming its fields', () => {
    const fixture = TestBed.createComponent(CategoryManager);
    const component = fixture.componentInstance;
    const changed = vi.fn();
    component.categoriesChanged.subscribe(changed);
    fixture.detectChanges();

    component.beginCreate();
    component.form.setValue({
      name: '  Personal  ',
      description: '  Home tasks  ',
      color: '#B6F500',
    });
    component.save();

    const http = TestBed.inject(HttpTestingController);
    const request = http.expectOne(`${API_BASE_URL}/api/categories`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      name: 'Personal',
      description: 'Home tasks',
      color: '#B6F500',
    });
    request.flush({ id: 2, name: 'Personal' });
    expect(changed).toHaveBeenCalledTimes(1);
    http.verify();
  });

  it('does not send a category request when its name is missing', () => {
    const fixture = TestBed.createComponent(CategoryManager);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.beginCreate();
    component.form.controls.name.setValue('  ');

    component.save();

    expect(component.form.controls.name.touched).toBe(true);
    expect(component.form.invalid).toBe(true);
    TestBed.inject(HttpTestingController).expectNone(`${API_BASE_URL}/api/categories`);
  });
});
