import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ locals }) => {
	// Redirect to dashboard if logged in, otherwise to login
	if (locals.user) {
		throw redirect(303, '/dashboard');
	} else {
		throw redirect(303, '/login');
	}
};
