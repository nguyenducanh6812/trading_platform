// Auto-save draft portfolio data to localStorage and database
import { browser } from '$app/environment';
import { apiClient } from '$lib/api';
import type { CreatePortfolioRequest } from '$lib/types';

const DRAFT_KEY = 'portfolio_draft';
const DRAFT_TIMESTAMP_KEY = 'portfolio_draft_timestamp';
const DRAFT_EXPIRY = 24 * 60 * 60 * 1000; // 24 hours

export const draftStore = {
	/**
	 * Save draft portfolio data
	 */
	save(data: Partial<CreatePortfolioRequest>, portfolioId?: number): void {
		if (!browser) return;

		try {
			const key = portfolioId ? `${DRAFT_KEY}_${portfolioId}` : DRAFT_KEY;
			const timestampKey = portfolioId ? `${DRAFT_TIMESTAMP_KEY}_${portfolioId}` : DRAFT_TIMESTAMP_KEY;

			localStorage.setItem(key, JSON.stringify(data));
			localStorage.setItem(timestampKey, Date.now().toString());

			console.log('[Draft] Saved:', { portfolioId, data });
		} catch (error) {
			console.error('[Draft] Failed to save:', error);
		}
	},

	/**
	 * Load draft portfolio data
	 */
	load(portfolioId?: number): Partial<CreatePortfolioRequest> | null {
		if (!browser) return null;

		try {
			const key = portfolioId ? `${DRAFT_KEY}_${portfolioId}` : DRAFT_KEY;
			const timestampKey = portfolioId ? `${DRAFT_TIMESTAMP_KEY}_${portfolioId}` : DRAFT_TIMESTAMP_KEY;

			const timestamp = localStorage.getItem(timestampKey);
			if (timestamp) {
				const age = Date.now() - parseInt(timestamp);
				if (age > DRAFT_EXPIRY) {
					// Draft expired, clear it
					this.clear(portfolioId);
					return null;
				}
			}

			const data = localStorage.getItem(key);
			if (data) {
				const parsed = JSON.parse(data);
				console.log('[Draft] Loaded:', { portfolioId, data: parsed });
				return parsed;
			}
		} catch (error) {
			console.error('[Draft] Failed to load:', error);
		}

		return null;
	},

	/**
	 * Clear draft data
	 */
	clear(portfolioId?: number): void {
		if (!browser) return;

		try {
			const key = portfolioId ? `${DRAFT_KEY}_${portfolioId}` : DRAFT_KEY;
			const timestampKey = portfolioId ? `${DRAFT_TIMESTAMP_KEY}_${portfolioId}` : DRAFT_TIMESTAMP_KEY;

			localStorage.removeItem(key);
			localStorage.removeItem(timestampKey);

			console.log('[Draft] Cleared:', { portfolioId });
		} catch (error) {
			console.error('[Draft] Failed to clear:', error);
		}
	},

	/**
	 * Check if draft exists
	 */
	hasDraft(portfolioId?: number): boolean {
		if (!browser) return false;

		const key = portfolioId ? `${DRAFT_KEY}_${portfolioId}` : DRAFT_KEY;
		return localStorage.getItem(key) !== null;
	},

	/**
	 * Save draft to database (cloud storage)
	 * Uses the same portfolio API with status: 'DRAFT'
	 */
	async saveToDatabase(
		data: Partial<CreatePortfolioRequest>,
		portfolioId?: number
	): Promise<{ id: number }> {
		if (!browser) throw new Error('Cannot save to database outside browser');

		try {
			const endpoint = portfolioId ? `/portfolios/${portfolioId}` : '/portfolios';
			const method = portfolioId ? 'PUT' : 'POST';

			// Add status: 'DRAFT' and userId to the request
			const draftData = {
				...data,
				status: 'DRAFT',
				userId: data.userId || 'dev-user-123' // TODO: Get from auth context
			};

			const response = await apiClient<{ id: number }>(endpoint, {
				method,
				body: draftData
			});

			console.log('[Draft] Saved to database as DRAFT:', { portfolioId, response });
			return response;
		} catch (error) {
			console.error('[Draft] Failed to save to database:', error);
			throw error;
		}
	},

	/**
	 * Load draft from database (cloud storage)
	 * Loads portfolios with status: 'DRAFT'
	 */
	async loadFromDatabase(portfolioId?: number): Promise<Partial<CreatePortfolioRequest> | null> {
		if (!browser) return null;

		try {
			if (portfolioId) {
				// Load specific portfolio (must be a draft)
				const portfolio = await apiClient<any>(`/portfolios/${portfolioId}`);
				if (portfolio.status !== 'DRAFT') {
					console.warn('[Draft] Portfolio is not a draft:', portfolio.status);
					return null;
				}
				console.log('[Draft] Loaded from database:', { portfolioId, portfolio });
				return portfolio;
			} else {
				// Load all drafts for the user and get the latest
				const userId = 'dev-user-123'; // TODO: Get from auth context
				const portfolios = await apiClient<any[]>(`/portfolios?userId=${userId}&status=DRAFT`);

				if (portfolios.length === 0) return null;

				// Return the most recently updated draft
				const latestDraft = portfolios.sort(
					(a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
				)[0];

				console.log('[Draft] Loaded latest draft from database:', latestDraft);
				return latestDraft;
			}
		} catch (error) {
			console.error('[Draft] Failed to load from database:', error);
			return null;
		}
	},

	/**
	 * Delete draft from database
	 * Uses the same DELETE /portfolios/{id} endpoint
	 */
	async deleteFromDatabase(portfolioId: number): Promise<void> {
		if (!browser) return;

		try {
			await apiClient(`/portfolios/${portfolioId}`, {
				method: 'DELETE'
			});

			console.log('[Draft] Deleted from database:', { portfolioId });
		} catch (error) {
			console.error('[Draft] Failed to delete from database:', error);
			throw error;
		}
	}
};
