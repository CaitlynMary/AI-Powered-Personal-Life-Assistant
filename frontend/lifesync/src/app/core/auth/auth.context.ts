import { HttpContextToken } from '@angular/common/http';

/** Public auth calls must not send a bearer token or start a refresh. */
export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);

/** A request already retried after refresh must not refresh again. */
export const AUTH_RETRY = new HttpContextToken<boolean>(() => false);
