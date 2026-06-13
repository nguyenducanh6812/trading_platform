// Trading strategy types
import type { StrategyType } from './portfolio';

export interface Strategy {
	id: string;
	name: string;
	type: StrategyType;
	description: string;
	parameters: StrategyParameters;
	enabled: boolean;
}

export interface StrategyParameters {
	// Common parameters
	riskTolerance?: 'CONSERVATIVE' | 'MODERATE' | 'AGGRESSIVE';
	rebalancingFrequency?: 'DAILY' | 'WEEKLY' | 'MONTHLY';

	// EMA Trend Following parameters
	fastEma?: number;
	slowEma?: number;

	// Mean Reversion RSI parameters
	rsiPeriod?: number;
	overbought?: number;
	oversold?: number;

	// MPT parameters
	targetRiskFreeRate?: number;
	maxPositionSize?: number;
	minPositionSize?: number;
}

// Pre-built strategy templates
export const STRATEGY_TEMPLATES: Strategy[] = [
	{
		id: 'mpt-optimizer',
		name: 'Modern Portfolio Theory',
		type: 'MPT',
		description: 'Optimizes portfolio allocation based on expected returns and risk',
		parameters: {
			targetRiskFreeRate: 4.0,
			riskTolerance: 'MODERATE',
			rebalancingFrequency: 'WEEKLY',
			maxPositionSize: 0.3,
			minPositionSize: 0.05
		},
		enabled: true
	},
	{
		id: 'equal-weight',
		name: 'Equal Weight',
		type: 'EQUAL_WEIGHT',
		description: 'Allocates capital equally across all instruments',
		parameters: {
			riskTolerance: 'MODERATE',
			rebalancingFrequency: 'MONTHLY'
		},
		enabled: true
	},
	{
		id: 'market-cap-weight',
		name: 'Market Cap Weight',
		type: 'MARKET_CAP_WEIGHT',
		description: 'Allocates capital based on market capitalization',
		parameters: {
			riskTolerance: 'CONSERVATIVE',
			rebalancingFrequency: 'MONTHLY'
		},
		enabled: true
	},
	{
		id: 'momentum-strategy',
		name: 'Momentum Strategy',
		type: 'MOMENTUM',
		description: 'Allocates based on price momentum and trends',
		parameters: {
			riskTolerance: 'AGGRESSIVE',
			rebalancingFrequency: 'WEEKLY'
		},
		enabled: true
	}
];

// Strategy performance metrics
export interface StrategyPerformance {
	strategyId: string;
	portfolioId: number;
	totalReturn: number;
	sharpeRatio: number;
	maxDrawdown: number;
	winRate: number;
	profitFactor: number;
	avgDailyReturn: number;
	volatility: number;
	beta?: number;
	alpha?: number;
}

// Strategy signals
export type SignalType = 'BUY' | 'SELL' | 'HOLD';

export interface TradingSignal {
	instrumentCode: string;
	signal: SignalType;
	strength: number; // 0-100
	timestamp: string;
	reason: string;
	indicators?: Record<string, number>;
}
