import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HTTP_INTERCEPTORS
} from '@angular/common/http';
import { Observable } from 'rxjs';

import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly authService: AuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // The backend now relies on the authenticated user to decide which projects are
    // visible/accessible, so the bearer token must be attached to every backend route
    // (the API is permit-all with non-proactive auth, so a valid token is always safe).
    const token = this.authService.getAccessToken();
    if (!token) {
      return next.handle(req);
    }

    const apiBase = environment.backendApiUrl;

    // If backendApiUrl is relative (e.g. "/api"), avoid attaching the token to *all* relative
    // requests like assets. Only attach to backend routes.
    if (apiBase.startsWith('/')) {
      const isBackendRoute = req.url.startsWith('/api/') || req.url === '/api' || req.url.startsWith('/auth/');
      if (!isBackendRoute) {
        return next.handle(req);
      }
    } else {
      // Absolute base (e.g. "http://localhost:8080/api"): attach to /api/** and /auth/** on that host.
      const backendRoot = apiBase.replace(/\/api\/?$/, '');
      const isBackendRequest =
        req.url.startsWith(apiBase) || req.url.startsWith(`${backendRoot}/auth/`) || req.url === `${backendRoot}/auth`;
      if (!isBackendRequest) {
        return next.handle(req);
      }
    }

    return next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
}

export const AUTH_INTERCEPTOR_PROVIDER = {
  provide: HTTP_INTERCEPTORS,
  useClass: AuthInterceptor,
  multi: true
};
