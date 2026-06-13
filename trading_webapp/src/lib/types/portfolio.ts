// Portfolio-related types mapped from backend DTOs (PortfolioResponse.java)

export type PortfolioStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'CLOSED';

export type StrategyType =
	| 'MPT'              // Modern Portfolio Theory
	| 'EQUAL_WEIGHT'     // Equal weight allocation
	| 'MARKET_CAP_WEIGHT' // Market cap weighted
	| 'MOMENTUM'         // Momentum strategy
	| 'CUSTOM';          // Custom strategy

export type RiskTolerance = 'CONSERVATIVE' | 'MODERATE' | 'AGGRESSIVE';

export type RebalancingFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'MANUAL';

export interface Portfolio {
	id: number;
	name: string;
	description: string | null;
	userId: string;
	capital: CapitalInfo;
	positions: PositionInfo[];
	strategy: StrategyInfo;
	leverage: LeverageInfo;
	status: PortfolioStatus;
	performance: PerformanceMetrics;
	lastRebalancedAt: string | null;
	createdAt: string;
	updatedAt: string;
}

export interface CapitalInfo {
	initial: number;
	current: number;
	available: number;
	reserved: number;
	currency: string;
	profitLoss: number;
	profitLossPercentage: number;
}

export interface PositionInfo {
	instrumentCode: string;
	instrumentName: string;
	marketType?: string; // Optional market type (SPOT, LINEAR, INVERSE, OPTION)
	quantity: number;
	weight?: number; // Optional allocation weight (0-1)
	averageEntryPrice: number;
	currentPrice?: number; // Alias for currentMarketPrice
	currentMarketPrice?: number; // Made optional for draft positions
	currentValue?: number; // Made optional for draft positions
	costBasis?: number; // Made optional for draft positions
	unrealizedPnL: number;
	unrealizedPnLPercentage?: number; // Made optional
	realizedPnL?: number; // Optional realized PnL
	status?: 'PENDING' | 'ACTIVE' | 'CLOSED'; // Optional status
	lastUpdated?: string; // Made optional
}

export interface StrategyInfo {
	type: StrategyType;
	riskTolerance: RiskTolerance;
	rebalancingFrequency: RebalancingFrequency;
	autoRebalance: boolean;
	targetRiskFreeRate: number;
}

export interface LeverageInfo {
	enabled: boolean;
	ratio: number;
	maxAllowed: number;
}

export interface PerformanceMetrics {
	totalValue: number;
	totalUnrealizedPnL: number;
	totalRealizedPnL: number;
	numberOfPositions: number;
	numberOfTrades: number;
}

// Request DTOs
export interface CreatePortfolioRequest {
	// Required fields (must match backend CreatePortfolioRequest.java)
	name: string; // 3-100 chars
	currency: string; // e.g., 'USD', 'USDT'
	initialCapital: number; // min 0.01
	symbols: string[]; // min 1 item, e.g., ['BTCUSDT', 'ETHUSDT'] - instrument symbols
	strategyType: StrategyType;
	riskTolerance: RiskTolerance;
	rebalancingFrequency: RebalancingFrequency;

	// Optional fields
	description?: string; // 0-500 chars
	autoRebalance?: boolean;
	leverageRatio?: number; // 1-125
	leverageEnabled?: boolean;
	status?: PortfolioStatus; // 'DRAFT' or 'ACTIVE' - determines if portfolio is saved as draft or deployed

	// Frontend-only fields (not sent to backend)
	positions?: PositionInfo[]; // Used internally in wizard for weight allocation
}

export interface UpdatePortfolioRequest {
	name?: string;
	description?: string;
	status?: PortfolioStatus;
	strategyType?: StrategyType;
	riskTolerance?: RiskTolerance;
	rebalancingFrequency?: RebalancingFrequency;
	autoRebalance?: boolean;
	leverageRatio?: number;
	leverageEnabled?: boolean;
}

export interface AddInstrumentRequest {
	instrumentCode: string;
}

export interface ExecuteTradeRequest {
	instrumentCode: string;
	action: 'BUY' | 'SELL';
	quantity: number;
	price?: number; // Optional, uses market price if not provided
}

// Response DTOs
export interface PortfolioListResponse {
	portfolios: Portfolio[];
	total: number;
}

export interface PortfolioStatsResponse {
	totalCapital: number;
	activePortfolios: number;
	draftPortfolios: number;
	totalUnrealizedPnL: number;
	totalRealizedPnL: number;
}
