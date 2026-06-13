// Market Data API endpoints

import { apiClient, createServerApiClient } from '../client';
import type {
	MarketInstrument,
	MarketDataRequest,
	MarketDataResponse,
	AvailableInstrumentsResponse,
	DataSufficiencyResponse,
	TradingInstrument
} from '$lib/types';

/**
 * Client-side Market Data API
 */
export const marketDataApi = {
	/**
	 * Fetch historical market data (async operation)
	 * Returns 202 Accepted
	 */
	async fetchHistorical(request: MarketDataRequest): Promise<{ taskId: string }> {
		return apiClient('/market-data/fetch-historical', {
			method: 'POST',
			body: request
		});
	},

	/**
	 * Convenience method to fetch BTC and ETH historical data
	 */
	async fetchBtcEthHistorical(): Promise<{ taskId: string }> {
		return apiClient('/market-data/fetch-btc-eth-historical', {
			method: 'POST'
		});
	},

	/**
	 * Get all available trading instruments
	 */
	async getInstruments(source: string = 'bybit'): Promise<AvailableInstrumentsResponse> {
		return apiClient('/market-data/instruments', {
			params: { source }
		});
	},

	/**
	 * Get specific instrument details
	 */
	async getInstrument(symbol: TradingInstrument): Promise<MarketInstrument> {
		return apiClient(`/market-data/instruments/${symbol}`);
	},

	/**
	 * Check if instrument has sufficient data for analysis
	 */
	async checkDataSufficiency(symbol: TradingInstrument): Promise<DataSufficiencyResponse> {
		return apiClient(`/market-data/instruments/${symbol}/data-sufficiency`);
	},

	/**
	 * Health check for market data service
	 */
	async health(): Promise<{ status: string }> {
		return apiClient('/market-data/health');
	}
};

/**
 * Server-side Market Data API
 */
export const createServerMarketDataApi = (fetch: typeof globalThis.fetch) => {
	const client = createServerApiClient(fetch);

	return {
		async fetchHistorical(request: MarketDataRequest): Promise<{ taskId: string }> {
			return client('/market-data/fetch-historical', {
				method: 'POST',
				body: request
			});
		},

		async fetchBtcEthHistorical(): Promise<{ taskId: string }> {
			return client('/market-data/fetch-btc-eth-historical', {
				method: 'POST'
			});
		},

		async getInstruments(source: string = 'bybit'): Promise<AvailableInstrumentsResponse> {
			return client('/market-data/instruments', {
				params: { source }
			});
		},

		async getInstrument(symbol: TradingInstrument): Promise<MarketInstrument> {
			return client(`/market-data/instruments/${symbol}`);
		},

		async checkDataSufficiency(symbol: TradingInstrument): Promise<DataSufficiencyResponse> {
			return client(`/market-data/instruments/${symbol}/data-sufficiency`);
		},

		async health(): Promise<{ status: string }> {
			return client('/market-data/health');
		}
	};
};
