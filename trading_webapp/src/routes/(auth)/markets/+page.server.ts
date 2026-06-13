import type { PageServerLoad } from './$types';
import { redirect } from '@sveltejs/kit';
import { createServerMarketDataApi } from '$lib/api';
import type { MarketInstrumentUI } from '$lib/types';

export const load: PageServerLoad = async ({ locals, fetch, depends }) => {
	if (!locals.user) {
		throw redirect(303, '/login');
	}

	depends('app:markets');

	const marketDataApi = createServerMarketDataApi(fetch);

	try {
		// Fetch available instruments
		const response = await marketDataApi.getInstruments();

		return {
			user: locals.user,
			instruments: response.instruments
		};
	} catch (error) {
		console.error('Failed to load market data:', error);

		// Return mock data for development
		const mockInstruments: MarketInstrumentUI[] = [
			{
				symbol: 'BTC',
				name: 'Bitcoin',
				currentPrice: 43250.75,
				priceChange24h: 2.45,
				high24h: 44100.0,
				low24h: 42800.0,
				volume24h: 28500000000,
				marketCap: 850000000000,
				icon: '₿'
			},
			{
				symbol: 'ETH',
				name: 'Ethereum',
				currentPrice: 2285.32,
				priceChange24h: -1.23,
				high24h: 2320.0,
				low24h: 2260.0,
				volume24h: 12400000000,
				marketCap: 275000000000,
				icon: 'Ξ'
			},
			{
				symbol: 'BNB',
				name: 'Binance Coin',
				currentPrice: 312.45,
				priceChange24h: 3.67,
				high24h: 318.0,
				low24h: 305.0,
				volume24h: 1800000000,
				marketCap: 48000000000,
				icon: 'BNB'
			},
			{
				symbol: 'ADA',
				name: 'Cardano',
				currentPrice: 0.485,
				priceChange24h: -0.92,
				high24h: 0.495,
				low24h: 0.475,
				volume24h: 420000000,
				marketCap: 17000000000,
				icon: 'ADA'
			},
			{
				symbol: 'SOL',
				name: 'Solana',
				currentPrice: 98.75,
				priceChange24h: 5.23,
				high24h: 102.0,
				low24h: 94.5,
				volume24h: 2100000000,
				marketCap: 42000000000,
				icon: 'SOL'
			},
			{
				symbol: 'DOT',
				name: 'Polkadot',
				currentPrice: 6.82,
				priceChange24h: 1.45,
				high24h: 6.95,
				low24h: 6.70,
				volume24h: 320000000,
				marketCap: 9500000000,
				icon: 'DOT'
			},
			{
				symbol: 'MATIC',
				name: 'Polygon',
				currentPrice: 0.875,
				priceChange24h: -2.15,
				high24h: 0.905,
				low24h: 0.860,
				volume24h: 450000000,
				marketCap: 8200000000,
				icon: 'MATIC'
			},
			{
				symbol: 'AVAX',
				name: 'Avalanche',
				currentPrice: 36.45,
				priceChange24h: 4.12,
				high24h: 37.8,
				low24h: 35.2,
				volume24h: 680000000,
				marketCap: 14000000000,
				icon: 'AVAX'
			}
		];

		return {
			user: locals.user,
			instruments: mockInstruments,
			error: 'Using mock market data. Backend connection failed.'
		};
	}
};
