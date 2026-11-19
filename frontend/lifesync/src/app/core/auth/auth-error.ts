import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorBody } from './auth.models';

export function readApiError(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return 'Cannot reach the LifeSync server. Make sure the backend is running on port 8080.';
  }

  const body = error.error as ApiErrorBody | null;
  const fieldErrors = body?.validationErrors;
  if (fieldErrors && Object.keys(fieldErrors).length > 0) {
    return Object.values(fieldErrors).join(' ');
  }

  if (typeof body?.message === 'string' && body.message.trim().length > 0) {
    return body.message;
  }

  return fallback;
}
