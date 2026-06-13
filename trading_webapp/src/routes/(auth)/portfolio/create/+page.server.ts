import type { PageServerLoad, Actions } from './$types';
import { redirect, fail } from '@sveltejs/kit';
import { createServerPortfolioApi } from '$lib/api';
import type {
	CreatePortfolioRequest,
	StrategyType,
	RiskTolerance,
	RebalancingFrequency,
	PortfolioStatus
} from '$lib/types';

export const load: PageServerLoad = async ({ locals }) => {
	if (!locals.user) {
		throw redirect(303, '/login');
	}

	return {
		user: locals.user
	};
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
				description: (formData.get('description') as string) || '',
				initialCapital: parseFloat(formData.get('initialCapital') as string),
				currency: (formData.get('currency') as string) || 'USD',
				symbols, // Required by backend API
				strategyType: formData.get('strategyType') as StrategyType,
				riskTolerance: formData.get('riskTolerance') as RiskTolerance,
				rebalancingFrequency: formData.get('rebalancingFrequency') as RebalancingFrequency,
				autoRebalance: formData.get('autoRebalance') === 'true',
				leverageRatio: parseInt(formData.get('leverageRatio') as string) || 1,
				leverageEnabled: formData.get('leverageEnabled') === 'true',
				status // 'DRAFT' or 'ACTIVE'
			};

			console.log('[Portfolio Create] Creating portfolio:', {
				...data,
				userId: locals.user.id,
				symbolsCount: symbols.length,
				symbols
			});

			const portfolio = await portfolioApi.create(data, locals.user.id);

			console.log('[Portfolio Create] Successfully created portfolio:', portfolio.id);

			return {
				success: true,
				portfolio
			};
		} catch (error: any) {
			console.error('='.repeat(80));
			console.error('[Portfolio Create] Failed to create portfolio');
			console.error('Error Type:', error.cause?.code || error.code || error.name);
			console.error('Message:', error.message);
			console.error('Status:', error.status || error.statusCode);
			console.error('Stack:', error.stack);
			console.error('='.repeat(80));

			// Return detailed error for global toast notification
			return fail(error.status || 500, {
				error: error.message || 'Failed to create portfolio. Please try again.',
				errorDetails: {
					endpoint: `${process.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'}/portfolios`,
					method: 'POST',
					errorType: error.cause?.code || error.code || error.name || 'FETCH_ERROR',
					statusCode: error.status || error.statusCode || 500,
					timestamp: new Date().toISOString(),
					message: error.message || 'Unknown error',
					stack: error.stack ? error.stack.split('\n').slice(0, 8).join('\n') : 'No stack trace',
					response: error.response // Include backend response if available
				}
			});
		}
	}
};
