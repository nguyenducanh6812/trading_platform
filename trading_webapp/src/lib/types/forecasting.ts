// Forecasting types mapped from backend DTOs (ForecastResponse.java)

import type { TradingInstrument } from './market';

export interface ForecastRequest {
	instrumentCode: TradingInstrument;
	startDate: string; // YYYY-MM-DD
	endDate: string; // YYYY-MM-DD
	isCurrentDate: boolean; // true for live, false for backtesting
	includeCalculationDetails?: boolean;
}

export interface ForecastResponse {
	instrumentCode: string;
	expectedReturn: number; // Decimal format (e.g., 0.05 = 5%)
	expectedReturnPercentage: number; // Percentage format (e.g., 5.0)
	confidenceLevel: number; // 0.0 - 1.0
	forecastDate: string;
	calculatedAt: string;
	calculationSteps?: CalculationStep[];
	metrics: ForecastMetrics;
	status: ForecastStatus;
	message?: string;
}

export interface CalculationStep {
	step: number;
	description: string;
	value: number;
	formula?: string;
}

export interface ForecastMetrics {
	arOrder: number;
	diffOrder: number;
	maOrder: number;
	aic: number; // Akaike Information Criterion
	bic: number; // Bayesian Information Criterion
	rmse: number; // Root Mean Square Error
	mae: number; // Mean Absolute Error
}

export type ForecastStatus = 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'PENDING';

export interface BatchForecastRequest {
	instruments: TradingInstrument[];
	startDate: string;
	endDate: string;
	isCurrentDate: boolean;
}

export interface BatchForecastResponse {
	forecasts: ForecastResponse[];
	successfulForecasts: number;
	failedInstruments: string[];
	executionTime: number; // milliseconds
}

export interface ForecastHealthResponse {
	status: 'UP' | 'DOWN';
	modelCacheSize: number;
	lastModelReload: string;
	availableModels: string[];
}

// Backtest-specific types
export interface BacktestForecast {
	date: string;
	instrumentCode: TradingInstrument;
	expectedReturn: number;
	actualReturn?: number;
	accuracy?: number;
}

export interface BacktestResult {
	portfolioId: number;
	startDate: string;
	endDate: string;
	initialCapital: number;
	finalCapital: number;
	totalReturn: number;
	totalReturnPercentage: number;
	sharpeRatio: number;
	maxDrawdown: number;
	maxDrawdownPercentage: number;
	winRate: number;
	totalTrades: number;
	profitableTrades: number;
	losingTrades: number;
	avgWin: number;
	avgLoss: number;
	profitFactor: number;
	equityCurve: EquityPoint[];
	trades: Trade[];
}

export interface EquityPoint {
	date: string;
	equity: number;
	drawdown: number;
}

export interface Trade {
	id: number;
	date: string;
	instrumentCode: TradingInstrument;
	action: 'BUY' | 'SELL';
	quantity: number;
	price: number;
	value: number;
	commission: number;
	pnl?: number;
	profitLoss?: number; // Alias for pnl (UI compatibility)
	timestamp?: string; // For backtest display
	type?: 'BUY' | 'SELL'; // Alias for action
	executionPrice?: number; // Actual execution price
}
