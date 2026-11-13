import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';

import { reviewedStepGuard } from './reviewed-step.guard';
import { StepReviewAccessService } from '../services/step-review-access.service';

describe('reviewedStepGuard', () => {
  let router: Router;
  let stepReviewAccess: jasmine.SpyObj<StepReviewAccessService>;

  beforeEach(() => {
    stepReviewAccess = jasmine.createSpyObj<StepReviewAccessService>('StepReviewAccessService', ['ensureProjectReviewState']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: StepReviewAccessService,
          useValue: stepReviewAccess
        }
      ]
    });

    router = TestBed.inject(Router);
  });

  it('redirects reviewed steps back to Step 6', async () => {
    stepReviewAccess.ensureProjectReviewState.and.returnValue(
      of({ step1: true, step2: false, step3: false, step4: false, step5: false })
    );

    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(
        reviewedStepGuard(
          {
            data: { reviewStepKey: 'step1' },
            queryParamMap: convertToParamMap({ projectId: '99' })
          } as never,
          { url: '/scope?projectId=99' } as never
        ) as Observable<boolean | UrlTree>
      )
    );

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/model-update?projectId=99');
  });

  it('allows navigation when the step is still pending review', async () => {
    stepReviewAccess.ensureProjectReviewState.and.returnValue(
      of({ step1: false, step2: false, step3: false, step4: false, step5: false })
    );

    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(
        reviewedStepGuard(
          {
            data: { reviewStepKey: 'step1' },
            queryParamMap: convertToParamMap({ projectId: '99' })
          } as never,
          { url: '/scope?projectId=99' } as never
        ) as Observable<boolean | UrlTree>
      )
    );

    expect(result).toBeTrue();
  });
});