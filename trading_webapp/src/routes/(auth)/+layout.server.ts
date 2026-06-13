import { redirect } from '@sveltejs/kit';
import type { LayoutServerLoad } from './$types';

export const load: LayoutServerLoad = async ({ locals }) => {
	// This is already checked in hooks.server.ts, but double-check for safety
	if (!locals.user) {
		throw redirect(303, '/login');
	}

	return {
		user: locals.user
	};
};
