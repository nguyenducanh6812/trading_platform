/**
 * API client for market data endpoints
 */

import { apiClient } from '../client';

export interface MarketResponse {
	id: number;
	code: string;
	name: string;
	description: string;
}

export interface InstrumentInfo {
	symbol: string;
	name: string;
	baseCurrency: string;
	quoteCurrency: string;
	dataPointCount: number;
	dataSource: string;
}

export interface GetInstrumentsByMarketRequest {
	marketId: number;
	marketCode: string;
	marketName: string;
}

export interface InstrumentsByMarketResponse {
	marketId: number;
	marketCode: string;
	marketName: string;
	instruments: InstrumentInfo[];
	totalInstruments: number;
}

/**
 * Get all available markets
 */
export async function getAllMarkets(): Promise<MarketResponse[]> {
	return await apiClient<MarketResponse[]>('/market-data/markets');
}

/**
 * Get instruments for a specific market
 * @param request - Request containing marketId, marketCode, and marketName
 */
export async function getInstrumentsByMarket(
	request: GetInstrumentsByMarketRequest
): Promise<InstrumentsByMarketResponse> {
	return await apiClient<InstrumentsByMarketResponse>('/market-data/markets/instruments', {
		method: 'POST',
		body: request
	});
}
