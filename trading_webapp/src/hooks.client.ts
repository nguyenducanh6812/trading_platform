import type { HandleClientError } from '@sveltejs/kit';

export const handleError: HandleClientError = ({ error, event }) => {
	console.error('Client error:', error, event);

	return {
		message: error instanceof Error ? error.message : 'An unexpected error occurred',
		code: (error as any)?.code ?? 'UNKNOWN'
	};
};
