import { fail, redirect } from '@sveltejs/kit';
import { signJWT, validateCredentials } from '$lib/server/auth';
import type { Actions, PageServerLoad } from './$types';

// Redirect if already logged in
export const load: PageServerLoad = async ({ locals }) => {
	if (locals.user) {
		throw redirect(303, '/dashboard');
	}
};

export const actions: Actions = {
	default: async ({ request, cookies }) => {
		const data = await request.formData();
		const username = data.get('username') as string;
		const password = data.get('password') as string;

		// Validate input
		if (!username || !password) {
			return fail(400, {
				username,
				error: 'Username and password are required'
			});
		}

		// Validate credentials
		const user = await validateCredentials(username, password);

		if (!user) {
			return fail(401, {
				username,
				error: 'Invalid username or password'
			});
		}

		try {
			// Sign JWT tokens
			const accessToken = await signJWT(user, '15m');
			const refreshToken = await signJWT({ id: user.id }, '7d');

			// Set HTTP-only cookies
			cookies.set('auth_token', accessToken, {
				path: '/',
				httpOnly: true,
				secure: process.env.NODE_ENV === 'production',
				sameSite: 'strict',
				maxAge: 60 * 15 // 15 minutes
			});

			cookies.set('refresh_token', refreshToken, {
				path: '/',
				httpOnly: true,
				secure: process.env.NODE_ENV === 'production',
				sameSite: 'strict',
				maxAge: 60 * 60 * 24 * 7 // 7 days
			});

			// Redirect to dashboard
			throw redirect(303, '/dashboard');
		} catch (e) {
			return fail(500, {
				username,
				error: 'An error occurred during login'
			});
		}
	}
};
