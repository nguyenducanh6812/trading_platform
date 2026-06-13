// Portfolio API endpoints

import { apiClient, createServerApiClient } from '../client';
import type {
	Portfolio,
	CreatePortfolioRequest,
	UpdatePortfolioRequest,
	AddInstrumentRequest,
	ExecuteTradeRequest,
	PortfolioStatsResponse
} from '$lib/types';

/**
 * Client-side Portfolio API
 * Use in components and client-side code
 */
export const portfolioApi = {
	/**
	 * Get all portfolios for a user
	 */
	async getAll(userId: string): Promise<Portfolio[]> {
		return apiClient('/portfolios', {
			params: { userId }
		});
	},

	/**
	 * Get only active portfolios for a user
	 */
	async getActive(userId: string): Promise<Portfolio[]> {
		return apiClient('/portfolios/active', {
			params: { userId }
		});
	},

	/**
	 * Get a specific portfolio by ID
	 */
	async getById(id: number): Promise<Portfolio> {
		return apiClient(`/portfolios/${id}`);
	},

	/**
	 * Create a new portfolio
	 */
	async create(data: CreatePortfolioRequest, userId: string): Promise<Portfolio> {
		return apiClient('/portfolios', {
			method: 'POST',
			params: { userId },
			body: data
		});
	},

	/**
	 * Update a portfolio
	 */
	async update(id: number, data: UpdatePortfolioRequest): Promise<Portfolio> {
		return apiClient(`/portfolios/${id}`, {
			method: 'PUT',
			body: data
		});
	},

	/**
	 * Delete a portfolio
	 */
	async delete(id: number): Promise<void> {
		return apiClient(`/portfolios/${id}`, {
			method: 'DELETE'
		});
	},

	/**
	 * Add an instrument to a portfolio
	 */
	async addInstrument(id: number, instrumentCode: string): Promise<Portfolio> {
		return apiClient(`/portfolios/${id}/instruments`, {
			method: 'POST',
			body: { instrumentCode } as AddInstrumentRequest
		});
	},

	/**
	 * Execute a trade
	 */
	async executeTrade(id: number, trade: ExecuteTradeRequest): Promise<Portfolio> {
		return apiClient(`/portfolios/${id}/trades`, {
			method: 'POST',
			body: trade
		});
	},

	/**
	 * Get portfolio statistics for dashboard
	 */
	async getStats(userId: string): Promise<PortfolioStatsResponse> {
		// This is a custom endpoint you might add to backend
		// For now, we'll calculate client-side from getAll
		const portfolios = await this.getAll(userId);

		const activePortfolios = portfolios.filter((p) => p.status === 'ACTIVE');
		const draftPortfolios = portfolios.filter((p) => p.status === 'DRAFT');

		const totalCapital = activePortfolios.reduce((sum, p) => sum + p.capital.current, 0);
		const totalUnrealizedPnL = activePortfolios.reduce(
			(sum, p) => sum + p.performance.totalUnrealizedPnL,
			0
		);
		const totalRealizedPnL = activePortfolios.reduce(
			(sum, p) => sum + p.performance.totalRealizedPnL,
			0
		);

		return {
			totalCapital,
			activePortfolios: activePortfolios.length,
			draftPortfolios: draftPortfolios.length,
			totalUnrealizedPnL,
			totalRealizedPnL
		};
	}
};

/**
 * Server-side Portfolio API
 * Use in load functions and form actions
 */
export const createServerPortfolioApi = (fetch: typeof globalThis.fetch) => {
	const client = createServerApiClient(fetch);

	return {
		async getAll(userId: string): Promise<Portfolio[]> {
			return client('/portfolios', { params: { userId } });
		},

		async getActive(userId: string): Promise<Portfolio[]> {
			return client('/portfolios/active', { params: { userId } });
		},

		async getById(id: number): Promise<Portfolio> {
			return client(`/portfolios/${id}`);
		},

		async create(data: CreatePortfolioRequest, userId: string): Promise<Portfolio> {
			return client('/portfolios', {
				method: 'POST',
				params: { userId },
				body: data
			});
		},

		async update(id: number, data: UpdatePortfolioRequest): Promise<Portfolio> {
			return client(`/portfolios/${id}`, {
				method: 'PUT',
				body: data
			});
		},

		async delete(id: number): Promise<void> {
			return client(`/portfolios/${id}`, {
				method: 'DELETE'
			});
		},

		async getStats(userId: string): Promise<PortfolioStatsResponse> {
			const portfolios = await this.getAll(userId);

			const activePortfolios = portfolios.filter((p) => p.status === 'ACTIVE');
			const draftPortfolios = portfolios.filter((p) => p.status === 'DRAFT');

			const totalCapital = activePortfolios.reduce((sum, p) => sum + p.capital.current, 0);
			const totalUnrealizedPnL = activePortfolios.reduce(
				(sum, p) => sum + p.performance.totalUnrealizedPnL,
				0
			);
			const totalRealizedPnL = activePortfolios.reduce(
				(sum, p) => sum + p.performance.totalRealizedPnL,
				0
			);

			return {
				totalCapital,
				activePortfolios: activePortfolios.length,
				draftPortfolios: draftPortfolios.length,
				totalUnrealizedPnL,
				totalRealizedPnL
			};
		}
	};
};
