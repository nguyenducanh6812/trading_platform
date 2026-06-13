/**
 * Portfolio default values and validation constants
 * Single source of truth for all default portfolio settings
 */

export const PORTFOLIO_DEFAULTS = {
	INITIAL_CAPITAL: 10000,
	STRATEGY_TYPE: 'MPT' as const,
	RISK_TOLERANCE: 'MODERATE' as const,
	REBALANCING_FREQUENCY: 'MONTHLY' as const,
	AUTO_REBALANCE: true,
	LEVERAGE_RATIO: 1,
	LEVERAGE_ENABLED: false,
	CURRENCY: 'USD' as const
} as const;

export const VALIDATION = {
	MIN_CAPITAL: 100,
	MIN_NAME_LENGTH: 3,
	MAX_NAME_LENGTH: 100,
	MIN_DESCRIPTION_LENGTH: 0,
	MAX_DESCRIPTION_LENGTH: 500,
	MIN_LEVERAGE: 1,
	MAX_LEVERAGE: 10,
	MIN_TOTAL_WEIGHT: 99,
	MAX_TOTAL_WEIGHT: 101,
	MIN_INSTRUMENTS: 1
} as const;

export const WIZARD_STEPS = {
	CORE_SETUP: 1,
	STRATEGY: 2,
	ASSETS: 3,
	BACKTEST: 4,
	TOTAL_STEPS: 4
} as const;
