import type { PageServerLoad, Actions } from './$types';
import { error, redirect, fail } from '@sveltejs/kit';
import { createServerPortfolioApi } from '$lib/api';
import type {
	UpdatePortfolioRequest,
	StrategyType,
	RiskTolerance,
	RebalancingFrequency
} from '$lib/types';

export const load: PageServerLoad = async ({ params, locals, fetch }) => {
	if (!locals.user) {
		throw redirect(303, '/login');
	}

	const portfolioId = parseInt(params.id);
	if (isNaN(portfolioId)) {
		throw error(400, 'Invalid portfolio ID');
	}

	const portfolioApi = createServerPortfolioApi(fetch);

	try {
		console.log(`[Portfolio Edit] Loading portfolio ${portfolioId} for user ${locals.user.id}`);
		const portfolios = await portfolioApi.getAll(locals.user.id);
		const portfolio = portfolios.find((p) => p.id === portfolioId);

		if (!portfolio) {
			throw error(404, 'Portfolio not found');
		}

		// Check ownership
		if (portfolio.userId !== locals.user.id) {
			throw error(403, 'Unauthorized to edit this portfolio');
		}

		console.log(`[Portfolio Edit] Successfully loaded portfolio:`, portfolio.name);

		return {
			user: locals.user,
			portfolio
		};
	} catch (err: any) {
		console.error('[Portfolio Edit] Failed to load portfolio:', err);

		if (err.status) {
			throw err; // Re-throw HTTP errors
		}

		throw error(500, 'Failed to load portfolio');
	}
};

export const actions: Actions = {
	update: async ({ request, params, locals, fetch }) => {
		if (!locals.user) {
			return fail(401, { error: 'Unauthorized' });
		}

		const portfolioId = parseInt(params.id);
		if (isNaN(portfolioId)) {
			return fail(400, { error: 'Invalid portfolio ID' });
		}

		const formData = await request.formData();
		const portfolioApi = createServerPortfolioApi(fetch);

		try {
			// Parse form data (update only modifiable fields)
			const data: UpdatePortfolioRequest = {
				name: formData.get('name') as string,
				description: (formData.get('description') as string) || '',
				strategyType: formData.get('strategyType') as StrategyType,
				riskTolerance: formData.get('riskTolerance') as RiskTolerance,
				rebalancingFrequency: formData.get('rebalancingFrequency') as RebalancingFrequency,
				autoRebalance: formData.get('autoRebalance') === 'true',
				leverageRatio: parseInt(formData.get('leverageRatio') as string) || 1,
				leverageEnabled: formData.get('leverageEnabled') === 'true'
			};

			console.log(`[Portfolio Update] Updating portfolio ${portfolioId}:`, data);

			const portfolio = await portfolioApi.update(portfolioId, data);

			console.log('[Portfolio Update] Successfully updated portfolio:', portfolio.id);

			return {
				success: true,
				portfolio
			};
		} catch (error: any) {
			console.error('='.repeat(80));
			console.error('[Portfolio Update] Failed to update portfolio');
			console.error('Error Type:', error.cause?.code || error.code || error.name);
			console.error('Message:', error.message);
			console.error('Status:', error.status || error.statusCode);
			console.error('Stack:', error.stack);
			console.error('='.repeat(80));

			return fail(500, {
				error: error.message || 'Failed to update portfolio. Please try again.',
				errorDetails: {
					endpoint: `${process.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'}/portfolios/${portfolioId}`,
					method: 'PUT',
					errorType: error.cause?.code || error.code || 'FETCH_ERROR',
					statusCode: error.status || error.statusCode || 500,
					timestamp: new Date().toISOString(),
					message: error.message || 'Unknown error',
					stack: error.stack ? error.stack.split('\n').slice(0, 8).join('\n') : 'No stack trace'
				}
			});
		}
	}
};
