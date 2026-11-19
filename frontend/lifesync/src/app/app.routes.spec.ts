import { routes } from './app.routes';

describe('routes', () => {
  it('serves the landing page publicly at the root before the guarded app shell', async () => {
    const landingRoute = routes.find((route) => route.path === '' && route.pathMatch === 'full');
    expect(landingRoute).toBeDefined();
    expect(landingRoute?.canActivate).toBeUndefined();
    expect(landingRoute?.loadComponent).toBeDefined();
    await expect(landingRoute?.loadComponent?.()).resolves.toBeTypeOf('function');
  });

  it('preserves the existing auth routes and protected dashboard route', () => {
    const login = routes.find((route) => route.path === 'login');
    const register = routes.find((route) => route.path === 'register');
    const protectedShell = routes.find((route) =>
      route.children?.some((child) => child.path === 'dashboard'),
    );

    expect(login?.canActivate).toBeDefined();
    expect(register?.canActivate).toBeDefined();
    expect(protectedShell?.canActivate).toBeDefined();
    expect(protectedShell?.children?.some((route) => route.path === 'dashboard')).toBe(true);
  });
});
