import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, of, tap } from 'rxjs';

import { ProjectService, StepSevenUpdatedStepKey, StepSevenUpdatedSteps } from './project.service';

type NormalizedUpdatedSteps = Record<StepSevenUpdatedStepKey, boolean>;

interface FullProjectReviewPayload extends Record<string, unknown> {
  step7ModelUpdate?: {
    updatedSteps?: StepSevenUpdatedSteps | null;
  } | null;
}

const EMPTY_UPDATED_STEPS: NormalizedUpdatedSteps = {
  step1: false,
  step2: false,
  step3: false,
  step4: false,
  step5: false
};

const REVIEWABLE_STEP_PATHS: Record<string, StepSevenUpdatedStepKey> = {
  '/scope': 'step1',
  '/istar-models': 'step2',
  '/control-structure': 'step3',
  '/ucas': 'step4',
  '/loss-scenarios': 'step5'
};

@Injectable({ providedIn: 'root' })
export class StepReviewAccessService {
  private readonly projectService = inject(ProjectService);

  private readonly activeProjectIdState = signal<number | null>(null);
  private readonly updatedStepsState = signal<NormalizedUpdatedSteps>(EMPTY_UPDATED_STEPS);

  readonly activeProjectId = this.activeProjectIdState.asReadonly();
  readonly updatedSteps = this.updatedStepsState.asReadonly();

  ensureProjectReviewState(projectId: number): Observable<NormalizedUpdatedSteps> {
    if (this.activeProjectIdState() === projectId) {
      return of(this.updatedStepsState());
    }

    return this.projectService.getFullProjectInformation(projectId).pipe(
      map((payload) => this.normalizeUpdatedSteps((payload as FullProjectReviewPayload).step7ModelUpdate?.updatedSteps)),
      tap((updatedSteps) => {
        this.activeProjectIdState.set(projectId);
        this.updatedStepsState.set(updatedSteps);
      })
    );
  }

  syncProjectReviewState(projectId: number, updatedSteps: StepSevenUpdatedSteps | null | undefined): void {
    this.activeProjectIdState.set(projectId);
    this.updatedStepsState.set(this.normalizeUpdatedSteps(updatedSteps));
  }

  clearProjectReviewState(): void {
    this.activeProjectIdState.set(null);
    this.updatedStepsState.set(EMPTY_UPDATED_STEPS);
  }

  getReviewKeyForPath(path: string): StepSevenUpdatedStepKey | null {
    return REVIEWABLE_STEP_PATHS[path] ?? null;
  }

  isPathBlocked(path: string): boolean {
    const stepKey = this.getReviewKeyForPath(path);

    return stepKey ? this.updatedStepsState()[stepKey] : false;
  }

  private normalizeUpdatedSteps(updatedSteps: StepSevenUpdatedSteps | null | undefined): NormalizedUpdatedSteps {
    return {
      step1: updatedSteps?.step1 ?? false,
      step2: updatedSteps?.step2 ?? false,
      step3: updatedSteps?.step3 ?? false,
      step4: updatedSteps?.step4 ?? false,
      step5: updatedSteps?.step5 ?? false
    };
  }
}