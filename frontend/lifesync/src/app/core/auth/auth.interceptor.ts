import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AUTH_RETRY, SKIP_AUTH } from './auth.context';
import { AuthService } from './auth.service';

function isPublicAuthUrl(url: string): boolean {
  return (
    url.includes('/api/auth/login') ||
    url.includes('/api/auth/refresh') ||
    url.includes('/api/auth/logout') ||
    url.includes('/api/users/register')
  );
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const skip = request.context.get(SKIP_AUTH) || isPublicAuthUrl(request.url);
  const token = auth.token();

  const outgoing =
    !skip && token
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || skip) {
        return throwError(() => error);
      }
      if (request.context.get(AUTH_RETRY)) {
        return throwError(() => error);
      }

      return from(auth.refreshAccessToken()).pipe(
        switchMap((newToken) => {
          if (!newToken) {
            void router.navigate(['/login']);
            return throwError(() => error);
          }
          const retry = request.clone({
            setHeaders: { Authorization: `Bearer ${newToken}` },
            context: request.context.set(AUTH_RETRY, true),
          });
          return next(retry);
        }),
      );
    }),
  );
};
