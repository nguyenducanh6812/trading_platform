import { redirect, type Handle } from '@sveltejs/kit';
import { verifyJWT, refreshAccessToken } from '$lib/server/auth';

// TEMPORARY: Disable auth for development
const AUTH_DISABLED = true;

export const handle: Handle = async ({ event, resolve }) => {
	// TEMPORARY: Mock user when auth is disabled
	if (AUTH_DISABLED) {
		event.locals.user = {
			id: 'dev-user-123',
			username: 'developer',
			email: 'dev@example.com'
		};
		return resolve(event);
	}

	// Get JWT tokens from cookies
	const accessToken = event.cookies.get('auth_token');
	const refreshToken = event.cookies.get('refresh_token');

	// Try to verify access token
	if (accessToken) {
		try {
			const user = await verifyJWT(accessToken);
			event.locals.user = user;
		} catch (e) {
			// Access token invalid/expired, try refresh token
			if (refreshToken) {
				try {
					const { token: newAccessToken, user } = await refreshAccessToken(refreshToken);

					// Set new access token
					event.cookies.set('auth_token', newAccessToken, {
						path: '/',
						httpOnly: true,
						secure: process.env.NODE_ENV === 'production',
						sameSite: 'strict',
						maxAge: 60 * 15 // 15 minutes
					});

					event.locals.user = user;
				} catch (refreshError) {
					// Refresh failed, clear cookies
					event.cookies.delete('auth_token', { path: '/' });
					event.cookies.delete('refresh_token', { path: '/' });
				}
			}
		}
	}

	// Protect authenticated routes
	const isAuthRoute = event.url.pathname.startsWith('/dashboard') ||
		event.url.pathname.startsWith('/portfolio') ||
		event.url.pathname.startsWith('/markets') ||
		event.url.pathname.startsWith('/backtest');

	if (isAuthRoute && !event.locals.user) {
		throw redirect(303, '/login');
	}

	// Redirect to dashboard if already logged in and trying to access login
	if (event.url.pathname === '/login' && event.locals.user) {
		throw redirect(303, '/dashboard');
	}

	return resolve(event);
};
