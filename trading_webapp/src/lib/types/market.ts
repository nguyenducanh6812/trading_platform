// Market data types mapped from backend DTOs

export type BybitMarketType = 'SPOT' | 'LINEAR' | 'INVERSE' | 'OPTION';

export type TradingInstrument =
	| 'BTC'
	| 'ETH'
	| 'BNB'
	| 'ADA'
	| 'SOL'
	| 'DOT'
	| 'MATIC'
	| 'AVAX';

export interface MarketInstrument {
	symbol: string;
	name: string;
	baseCurrency: string;
	quoteCurrency: string;
	priceHistory: OHLCV[];
	qualityMetrics: DataQualityMetrics;
	lastUpdated: string;
	firstTradingDate: string | null;
}

export interface OHLCV {
	timestamp: string;
	open: number;
	high: number;
	low: number;
	close: number;
	volume: number;
}

export interface DataQualityMetrics {
	totalDataPoints: number;
	missingDataPoints: number;
	completenessPercentage: number;
	lastQualityCheck: string;
}

export interface MarketDataRequest {
	instruments: TradingInstrument[];
	startDate: string; // YYYY-MM-DD
	endDate: string; // YYYY-MM-DD
	source: string; // "bybit", "binance", etc.
}

export interface MarketDataResponse {
	status: 'SUCCESS' | 'PARTIAL' | 'FAILED';
	instruments: MarketInstrument[];
	failedInstruments: string[];
	message: string;
}

export interface AvailableInstrumentsResponse {
	instruments: MarketInstrument[];
	source: string;
	total: number;
}

export interface DataSufficiencyResponse {
	symbol: string;
	hasSufficientData: boolean;
	availableDataPoints: number;
	requiredDataPoints: number;
	startDate: string | null;
	endDate: string | null;
	message: string;
}

// UI-specific market data (for display purposes)
export interface MarketInstrumentUI {
	symbol: string;
	name: string;
	currentPrice: number;
	priceChange24h: number;
	high24h: number;
	low24h: number;
	volume24h: number;
	marketCap: number;
	icon?: string;
}

// Client-side types for UI
export interface Asset {
	code: TradingInstrument;
	name: string;
	symbol: string;
	price: number;
	change24h: number;
	volume24h?: number;
	marketCap?: number;
	lastUpdated?: string;
}

// Mock data structure for development
export const CRYPTO_ASSETS: Asset[] = [
	{ code: 'BTC', name: 'Bitcoin', symbol: 'BTC', price: 43250.5, change24h: 2.34 },
	{ code: 'ETH', name: 'Ethereum', symbol: 'ETH', price: 2280.75, change24h: -0.89 }
];
