import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { StepSevenUpdatedStepKey } from '../services/project.service';
import { StepReviewAccessService } from '../services/step-review-access.service';

export const reviewedStepGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const reviewAccess = inject(StepReviewAccessService);

  const stepKey = route.data['reviewStepKey'] as StepSevenUpdatedStepKey | undefined;
  const projectIdParam = route.queryParamMap.get('projectId');
  const parsedProjectId = projectIdParam ? Number(projectIdParam) : null;
  const projectId = parsedProjectId && !Number.isNaN(parsedProjectId) ? parsedProjectId : null;

  if (!stepKey || !projectId) {
    return true;
  }

  return reviewAccess.ensureProjectReviewState(projectId).pipe(
    map((updatedSteps) =>
      updatedSteps[stepKey]
        ? router.createUrlTree(['/model-update'], { queryParams: { projectId } })
        : true
    ),
    catchError(() => of(true))
  );
};