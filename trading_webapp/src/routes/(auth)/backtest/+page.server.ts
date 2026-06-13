import type { PageServerLoad } from './$types';
import { redirect } from '@sveltejs/kit';
import type { BacktestResult } from '$lib/types';

export const load: PageServerLoad = async ({ locals, fetch, depends }) => {
	if (!locals.user) {
		throw redirect(303, '/login');
	}

	depends('app:backtest');

	try {
		// TODO: Implement actual backtest API call
		// const backtests = await backtestApi.getAll(locals.user.id);

		// Return mock data for development
		const mockBacktests: BacktestResult[] = [
			{
				id: '1',
				portfolioId: 1,
				portfolioName: 'Aggressive Crypto Portfolio',
				strategyType: 'MEAN_REVERSION',
				startDate: '2024-01-01',
				endDate: '2024-03-01',
				initialCapital: 10000,
				finalValue: 12350,
				totalReturn: 23.5,
				sharpeRatio: 1.45,
				maxDrawdown: -12.3,
				winRate: 58.2,
				totalTrades: 142,
				winningTrades: 83,
				losingTrades: 59,
				equityCurve: [],
				trades: [
					{
						timestamp: '2024-01-05T10:30:00Z',
						type: 'BUY',
						instrumentCode: 'BTC',
						quantity: 0.5,
						price: 42500,
						profitLoss: 0,
						executionPrice: 42500
					},
					{
						timestamp: '2024-01-08T14:20:00Z',
						type: 'SELL',
						instrumentCode: 'BTC',
						quantity: 0.5,
						price: 44200,
						profitLoss: 850,
						executionPrice: 44200
					},
					{
						timestamp: '2024-01-10T09:15:00Z',
						type: 'BUY',
						instrumentCode: 'ETH',
						quantity: 5,
						price: 2280,
						profitLoss: 0,
						executionPrice: 2280
					}
				]
			},
			{
				id: '2',
				portfolioId: 2,
				portfolioName: 'Conservative DeFi Strategy',
				strategyType: 'TREND_FOLLOWING',
				startDate: '2023-12-01',
				endDate: '2024-02-28',
				initialCapital: 25000,
				finalValue: 27800,
				totalReturn: 11.2,
				sharpeRatio: 1.12,
				maxDrawdown: -8.5,
				winRate: 62.5,
				totalTrades: 96,
				winningTrades: 60,
				losingTrades: 36,
				equityCurve: [],
				trades: []
			},
			{
				id: '3',
				portfolioId: 3,
				portfolioName: 'High Frequency Arbitrage',
				strategyType: 'STATISTICAL_ARBITRAGE',
				startDate: '2024-02-01',
				endDate: '2024-03-15',
				initialCapital: 50000,
				finalValue: 48200,
				totalReturn: -3.6,
				sharpeRatio: 0.85,
				maxDrawdown: -15.2,
				winRate: 48.3,
				totalTrades: 287,
				winningTrades: 139,
				losingTrades: 148,
				equityCurve: [],
				trades: []
			}
		];

		return {
			user: locals.user,
			backtests: mockBacktests
		};
	} catch (error) {
		console.error('Failed to load backtest history:', error);

		return {
			user: locals.user,
			backtests: [],
			error: 'Failed to load backtest history. Please try again later.'
		};
	}
};
