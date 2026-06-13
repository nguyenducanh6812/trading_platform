import type { PageServerLoad, Actions } from './$types';
import { fail, redirect } from '@sveltejs/kit';
import { createServerPortfolioApi } from '$lib/api';
import type {
	CreatePortfolioRequest,
	UpdatePortfolioRequest,
	StrategyType,
	RiskTolerance,
	RebalancingFrequency
} from '$lib/types';

export const load: PageServerLoad = async ({ locals, fetch }) => {
	// Always return valid data structure, never throw errors
	try {
		if (!locals.user) {
			throw redirect(303, '/login');
		}

		const portfolioApi = createServerPortfolioApi(fetch);

		try {
			console.log(`[Portfolio Load] Fetching portfolios for user: ${locals.user.id}`);
			const portfolios = await portfolioApi.getAll(locals.user.id);
			console.log(`[Portfolio Load] Successfully loaded ${portfolios.length} portfolios`);

			return {
				user: locals.user,
				portfolios
			};
		} catch (error: any) {
			// Log detailed error on server
			console.error('='.repeat(80));
			console.error('[Portfolio Load] API Error Details:');
			console.error('Error Type:', error.cause?.code || error.code || error.name);
			console.error('Message:', error.message);
			console.error('Status:', error.status || error.statusCode);
			console.error('Stack:', error.stack);
			console.error('='.repeat(80));

			// Extract detailed error information (serializable)
			const errorDetails = {
				endpoint: `${process.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'}/portfolios?userId=${locals.user.id}`,
				method: 'GET',
				errorType: error.cause?.code || error.code || error.name || 'FETCH_ERROR',
				statusCode: error.status || error.statusCode || 0,
				timestamp: new Date().toISOString(),
				message: error.message || 'Unknown error',
				stack: error.stack ? error.stack.split('\n').slice(0, 8).join('\n') : 'No stack trace available'
			};

			// Always return valid data - NEVER throw
			return {
				user: locals.user,
				portfolios: [],
				error: 'Failed to load portfolios. Backend may be offline or unreachable.',
				errorDetails
			};
		}
	} catch (error: any) {
		// Catch-all for any unexpected errors
		console.error('[Portfolio Load] Unexpected error:', error);

		return {
			user: locals.user,
			portfolios: [],
			error: 'An unexpected error occurred',
			errorDetails: {
				endpoint: 'unknown',
				method: 'GET',
				errorType: 'UNEXPECTED_ERROR',
				statusCode: 500,
				timestamp: new Date().toISOString(),
				message: error.message || 'Unknown error',
				stack: error.stack || 'No stack trace'
			}
		};
	}
};

export const actions: Actions = {
	create: async ({ request, locals, fetch }) => {
		if (!locals.user) {
			return fail(401, { error: 'Unauthorized' });
		}

		const formData = await request.formData();
		const portfolioApi = createServerPortfolioApi(fetch);

		try {
			// Parse form data (flat structure matching backend DTO)
			const symbolsRaw = formData.get('symbols') as string;
			const symbols = symbolsRaw ? JSON.parse(symbolsRaw) : [];
			const status = (formData.get('status') as 'DRAFT' | 'ACTIVE') || 'DRAFT';

			const data: CreatePortfolioRequest = {
				name: formData.get('name') as string,
				description: formData.get('description') as string || '',
				initialCapital: parseFloat(formData.get('initialCapital') as string),
				currency: formData.get('currency') as string || 'USD',
				symbols, // Required by backend API
				strategyType: formData.get('strategyType') as StrategyType,
				riskTolerance: formData.get('riskTolerance') as RiskTolerance,
				rebalancingFrequency: formData.get('rebalancingFrequency') as RebalancingFrequency,
				autoRebalance: formData.get('autoRebalance') === 'true',
				leverageRatio: parseInt(formData.get('leverageRatio') as string) || 1,
				leverageEnabled: formData.get('leverageEnabled') === 'true',
				status // 'DRAFT' or 'ACTIVE'
			};

			console.log('[Portfolio Create] Request data:', {
				...data,
				userId: locals.user.id,
				symbolsCount: symbols.length,
				symbols
			});

			const portfolio = await portfolioApi.create(data, locals.user.id);

			return {
				success: true,
				portfolio
			};
		} catch (error) {
			console.error('Failed to create portfolio:', error);
			return fail(500, {
				error: 'Failed to create portfolio. Please try again.'
			});
		}
	},

	update: async ({ request, fetch }) => {
		const formData = await request.formData();
		const portfolioApi = createServerPortfolioApi(fetch);

		try {
			const id = parseInt(formData.get('id') as string);
			const data: UpdatePortfolioRequest = {
				name: formData.get('name') as string,
				capital: JSON.parse(formData.get('capital') as string),
				leverage: JSON.parse(formData.get('leverage') as string),
				strategy: JSON.parse(formData.get('strategy') as string),
				positions: JSON.parse(formData.get('positions') as string)
			};

			const portfolio = await portfolioApi.update(id, data);

			return {
				success: true,
				portfolio
			};
		} catch (error) {
			console.error('Failed to update portfolio:', error);
			return fail(500, {
				error: 'Failed to update portfolio. Please try again.'
			});
		}
	},

	delete: async ({ request, fetch }) => {
		const formData = await request.formData();
		const portfolioApi = createServerPortfolioApi(fetch);

		try {
			const id = parseInt(formData.get('id') as string);
			await portfolioApi.delete(id);

			return {
				success: true
			};
		} catch (error) {
			console.error('Failed to delete portfolio:', error);
			return fail(500, {
				error: 'Failed to delete portfolio. Please try again.'
			});
		}
	},

	activate: async ({ request, fetch }) => {
		const formData = await request.formData();
		const portfolioApi = createServerPortfolioApi(fetch);

		try {
			const id = parseInt(formData.get('id') as string);
			// TODO: Implement activate API endpoint
			// const portfolio = await portfolioApi.activate(id);

			return {
				success: true,
				message: 'Portfolio activation coming soon'
			};
		} catch (error) {
			console.error('Failed to activate portfolio:', error);
			return fail(500, {
				error: 'Failed to activate portfolio. Please try again.'
			});
		}
	}
};
