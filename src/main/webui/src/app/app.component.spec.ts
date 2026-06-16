import { Component, signal } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { AppComponent } from './app.component';
import { AuthService } from './services/auth.service';
import { ProjectService } from './services/project.service';
import { StepReviewAccessService } from './services/step-review-access.service';

@Component({
  standalone: true,
  selector: 'app-dashboard-stub',
  template: '<p>Dashboard</p>'
})
class DashboardStubComponent {}

@Component({
  standalone: true,
  selector: 'app-scope-stub',
  template: '<p>Scope</p>'
})
class ScopeStubComponent {}

@Component({
  standalone: true,
  selector: 'app-model-update-summary-stub',
  template: '<p>Model Update Summary</p>'
})
class ModelUpdateSummaryStubComponent {}

describe('AppComponent', () => {
  let authState: ReturnType<typeof signal<boolean>>;
  let projectService: jasmine.SpyObj<ProjectService>;

  beforeEach(async () => {
    authState = signal(false);
    projectService = jasmine.createSpyObj<ProjectService>('ProjectService', ['getFullProjectInformation']);
    projectService.getFullProjectInformation.and.returnValue(of({ step7ModelUpdate: { updatedSteps: {} } }));

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([
          { path: '', component: DashboardStubComponent },
          { path: 'scope', component: ScopeStubComponent },
          { path: 'model-update', component: ModelUpdateSummaryStubComponent }
        ]),
        provideHttpClient(withFetch()),
        provideHttpClientTesting(),
        {
          provide: ProjectService,
          useValue: projectService
        },
        {
          provide: AuthService,
          useValue: {
            authState,
            signOut: jasmine.createSpy('signOut')
          }
        }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should have the resafety_tool title', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.appTitle).toEqual('resafety_tool');
  });

  it('should render title', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('resafety_tool');
  });

  it('hides the lateral menu on the dashboard even when query params are preserved', fakeAsync(() => {
    authState.set(true);

    const fixture = TestBed.createComponent(AppComponent);
    const router = TestBed.inject(Router);

    fixture.detectChanges();

    fixture.ngZone?.run(() => {
      void router.navigateByUrl('/scope?projectId=123');
    });
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.shell__nav')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.shell__menu')).not.toBeNull();

    fixture.ngZone?.run(() => {
      void router.navigateByUrl('/?projectId=123');
    });
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.shell__nav')).toBeNull();
    expect(fixture.nativeElement.querySelector('.shell__menu')).toBeNull();
  }));

  it('keeps reviewed workflow steps visible but blocked in the sidebar', fakeAsync(() => {
    authState.set(true);

    const fixture = TestBed.createComponent(AppComponent);
    const router = TestBed.inject(Router);
    const stepReviewAccess = TestBed.inject(StepReviewAccessService);

    fixture.detectChanges();

    fixture.ngZone?.run(() => {
      void router.navigateByUrl('/model-update?projectId=123');
    });
    tick();
    fixture.detectChanges();

    stepReviewAccess.syncProjectReviewState(123, { step1: true });
    fixture.detectChanges();

    const navLinks = Array.from(fixture.nativeElement.querySelectorAll('.shell__nav a')) as HTMLAnchorElement[];
    const scopeLink = navLinks.find((element) => element.dataset['navPath'] === '/scope');

    expect(scopeLink).toBeDefined();
    expect(scopeLink?.classList.contains('nav-link--blocked')).toBeTrue();
    expect(scopeLink?.getAttribute('aria-disabled')).toBe('true');

    scopeLink?.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    tick();
    fixture.detectChanges();

    expect(router.url).toBe('/model-update?projectId=123');
  }));
});
