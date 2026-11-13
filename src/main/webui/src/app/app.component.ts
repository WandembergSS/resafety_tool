import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { EMPTY, catchError } from 'rxjs';

import { AuthService } from './services/auth.service';
import { StepReviewAccessService } from './services/step-review-access.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  readonly appTitle = 'RESafety Tool';
  readonly appSubtitle = '6-Step ReSafety Workflow';
  readonly navOpen = signal(false);
  readonly isNavigating = signal(false);
  readonly currentUrl = signal('');
  readonly currentProjectId = signal<number | null>(null);
  readonly hasCompletedInitialNavigation = signal(false);

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly stepReviewAccess = inject(StepReviewAccessService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isAuthenticated = this.authService.authState;
  readonly isDashboardRoute = computed(() => this.isRootUrl(this.currentUrl()));
  readonly isHelpRoute = computed(() => this.isHelpUrl(this.currentUrl()));
  readonly showWorkflowNav = computed(
    () => this.isAuthenticated() && !this.isDashboardRoute() && !this.isHelpRoute()
  );

  readonly navSections = [
    {
      heading: 'Workspace',
      links: [
        {
          path: '/',
          label: 'Home Dashboard',
          description: 'Portfolio of safety analyses and setup'
        }
      ]
    },
    {
      heading: 'RESafety Workflow',
      links: [
        {
          path: '/scope',
          label: 'Step 1 · Define SCS Scope',
          description: 'Define mission, hazards, and baseline constraints'
        },
        {
          path: '/istar-models',
          label: 'Step 2 · iStar4Safety Models',
          description: 'Map actors, responsibilities, and safety goals'
        },
        {
          path: '/control-structure',
          label: 'Step 3 · Control Structure',
          description: 'Translate goals into STPA control loops'
        },
        {
          path: '/ucas',
          label: 'Step 4 · Unsafe Control Actions & Hazardous Conditions',
          description: 'Evaluate STPA contexts and map unsafe conditions to hazards'
        },
        {
          path: '/loss-scenarios',
          label: 'Step 5 · Loss Scenarios & Safety Requirements',
          description: 'Assess causal scenarios and derive mitigations'
        },
        {
          path: '/model-update',
          label: 'Step 6 · Update iStar4Safety Models',
          description: 'Review completed steps and generate ReSafety artifacts'
        }
      ]
    }
  ];

  constructor() {
    this.currentUrl.set(this.router.url);
    this.syncProjectReviewAccess(this.router.url);

    this.router.events.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
      if (event instanceof NavigationStart) {
        const shouldShowSkeleton =
          this.hasCompletedInitialNavigation() && event.url !== this.currentUrl();

        this.isNavigating.set(shouldShowSkeleton);
        return;
      }

      if (
        event instanceof NavigationEnd ||
        event instanceof NavigationCancel ||
        event instanceof NavigationError
      ) {
        this.isNavigating.set(false);

        if (event instanceof NavigationEnd) {
          this.currentUrl.set(event.urlAfterRedirects);
          this.syncProjectReviewAccess(event.urlAfterRedirects);

          if (this.isDashboardRoute() || this.isHelpRoute()) {
            this.closeNav();
          }
        }

        if (!this.hasCompletedInitialNavigation()) {
          this.hasCompletedInitialNavigation.set(true);
        }
      }
    });
  }

  toggleNav(): void {
    this.navOpen.update((value) => !value);
  }

  isNavLinkBlocked(path: string): boolean {
    const currentProjectId = this.currentProjectId();

    if (!currentProjectId) {
      return false;
    }

    return this.stepReviewAccess.activeProjectId() === currentProjectId && this.stepReviewAccess.isPathBlocked(path);
  }

  getNavLinkTitle(description: string, path: string): string {
    if (!this.isNavLinkBlocked(path)) {
      return description;
    }

    return 'This step was marked as reviewed on Step 6. Open Step 6 and mark it pending review before editing it again.';
  }

  handleNavLinkClick(event: MouseEvent, path: string): void {
    if (this.isNavLinkBlocked(path)) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }

    this.closeNav();
  }

  closeNav(): void {
    this.navOpen.set(false);
  }

  logout(): void {
    this.authService.signOut();
    this.closeNav();
    this.router.navigate(['/login']);
  }

  private isRootUrl(url: string): boolean {
    const primarySegments = this.router.parseUrl(url || '/').root.children['primary']?.segments ?? [];

    return primarySegments.length === 0;
  }

  private isHelpUrl(url: string): boolean {
    const primarySegments = this.router.parseUrl(url || '/').root.children['primary']?.segments ?? [];

    return primarySegments.length > 0 && primarySegments[0]?.path === 'help';
  }

  private syncProjectReviewAccess(url: string): void {
    const projectId = this.getProjectIdFromUrl(url);

    this.currentProjectId.set(projectId);

    if (!projectId || this.isRootUrl(url) || this.isHelpUrl(url)) {
      this.stepReviewAccess.clearProjectReviewState();
      return;
    }

    this.stepReviewAccess
      .ensureProjectReviewState(projectId)
      .pipe(
        catchError((error) => {
          console.error(`Failed to load reviewed-step state for project ${projectId}.`, error);
          this.stepReviewAccess.clearProjectReviewState();
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private getProjectIdFromUrl(url: string): number | null {
    const projectIdParam = this.router.parseUrl(url || '/').queryParamMap.get('projectId');
    const parsedProjectId = projectIdParam ? Number(projectIdParam) : null;

    return parsedProjectId && !Number.isNaN(parsedProjectId) ? parsedProjectId : null;
  }
}
