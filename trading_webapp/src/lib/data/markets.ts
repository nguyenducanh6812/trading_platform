/**
 * Centralized market and instrument data
 * This file contains all static reference data for portfolio creation
 */

import type { BybitMarketType, TradingInstrument } from '$lib/types';

// ============================================================================
// TYPES
// ============================================================================

export interface InstrumentData {
	code: TradingInstrument;
	name: string;
	description: string;
	icon: string;
}

export interface MarketTypeInfo {
	type: BybitMarketType;
	name: string;
	description: string;
	enabled: boolean;
}

// ============================================================================
// MARKET TYPES
// ============================================================================

export const MARKET_TYPES: Record<BybitMarketType, MarketTypeInfo> = {
	SPOT: {
		type: 'SPOT',
		name: 'Spot Trading',
		description: 'Buy and sell cryptocurrencies directly',
		enabled: true
	},
	LINEAR: {
		type: 'LINEAR',
		name: 'USDT Perpetual',
		description: 'USDT-margined perpetual futures contracts',
		enabled: true
	},
	INVERSE: {
		type: 'INVERSE',
		name: 'Coin Perpetual',
		description: 'Coin-margined inverse perpetual contracts',
		enabled: true
	},
	OPTION: {
		type: 'OPTION',
		name: 'Options',
		description: 'Cryptocurrency options trading',
		enabled: true
	}
};

// ============================================================================
// INSTRUMENTS BY MARKET TYPE
// ============================================================================

export const INSTRUMENTS_BY_MARKET: Record<BybitMarketType, InstrumentData[]> = {
	// SPOT Market Instruments
	SPOT: [
		{
			code: 'BTC',
			name: 'Bitcoin',
			description: 'BTC/USDT spot trading',
			icon: '₿'
		},
		{
			code: 'ETH',
			name: 'Ethereum',
			description: 'ETH/USDT spot trading',
			icon: 'Ξ'
		}
	],

	// LINEAR (USDT Perpetual) Market Instruments
	LINEAR: [
		{
			code: 'BTC',
			name: 'Bitcoin',
			description: 'USDT-margined BTC perpetual',
			icon: '₿'
		},
		{
			code: 'ETH',
			name: 'Ethereum',
			description: 'USDT-margined ETH perpetual',
			icon: 'Ξ'
		},
		{
			code: 'BNB',
			name: 'Binance Coin',
			description: 'USDT-margined BNB perpetual',
			icon: 'BNB'
		},
		{
			code: 'ADA',
			name: 'Cardano',
			description: 'USDT-margined ADA perpetual',
			icon: 'ADA'
		},
		{
			code: 'SOL',
			name: 'Solana',
			description: 'USDT-margined SOL perpetual',
			icon: 'SOL'
		},
		{
			code: 'DOT',
			name: 'Polkadot',
			description: 'USDT-margined DOT perpetual',
			icon: 'DOT'
		},
		{
			code: 'MATIC',
			name: 'Polygon',
			description: 'USDT-margined MATIC perpetual',
			icon: 'MATIC'
		},
		{
			code: 'AVAX',
			name: 'Avalanche',
			description: 'USDT-margined AVAX perpetual',
			icon: 'AVAX'
		}
	],

	// INVERSE (Coin Perpetual) Market Instruments
	INVERSE: [
		{
			code: 'BTC',
			name: 'Bitcoin',
			description: 'Coin-margined BTC inverse perpetual',
			icon: '₿'
		},
		{
			code: 'ETH',
			name: 'Ethereum',
			description: 'Coin-margined ETH inverse perpetual',
			icon: 'Ξ'
		}
	],

	// OPTION Market Instruments
	OPTION: [
		{
			code: 'BTC',
			name: 'Bitcoin',
			description: 'BTC options contracts',
			icon: '₿'
		},
		{
			code: 'ETH',
			name: 'Ethereum',
			description: 'ETH options contracts',
			icon: 'Ξ'
		}
	]
};

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Get all available market types
 */
export function getAvailableMarketTypes(): MarketTypeInfo[] {
	return Object.values(MARKET_TYPES).filter((m) => m.enabled);
}

/**
 * Get instruments for a specific market type
 */
export function getInstrumentsForMarket(marketType: BybitMarketType): InstrumentData[] {
	return INSTRUMENTS_BY_MARKET[marketType] || [];
}

/**
 * Get all unique instrument codes across all markets
 */
export function getAllInstrumentCodes(): TradingInstrument[] {
	const codes = new Set<TradingInstrument>();
	Object.values(INSTRUMENTS_BY_MARKET).forEach((instruments) => {
		instruments.forEach((inst) => codes.add(inst.code));
	});
	return Array.from(codes).sort();
}

/**
 * Find instrument by code in a specific market
 */
export function findInstrument(
	code: TradingInstrument,
	marketType: BybitMarketType
): InstrumentData | undefined {
	return INSTRUMENTS_BY_MARKET[marketType]?.find((inst) => inst.code === code);
}

/**
 * Check if instrument is available in market
 */
export function isInstrumentAvailable(
	code: TradingInstrument,
	marketType: BybitMarketType
): boolean {
	return INSTRUMENTS_BY_MARKET[marketType]?.some((inst) => inst.code === code) || false;
}
