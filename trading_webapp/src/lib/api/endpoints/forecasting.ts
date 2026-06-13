// Forecasting API endpoints

import { apiClient, createServerApiClient } from '../client';
import type {
	ForecastRequest,
	ForecastResponse,
	BatchForecastRequest,
	BatchForecastResponse,
	ForecastHealthResponse,
	TradingInstrument
} from '$lib/types';

/**
 * Client-side Forecasting API
 */
export const forecastingApi = {
	/**
	 * Execute ARIMA forecast (async)
	 */
	async execute(request: ForecastRequest): Promise<ForecastResponse> {
		return apiClient('/forecasting/execute', {
			method: 'POST',
			body: request
		});
	},

	/**
	 * Execute ARIMA forecast (synchronous)
	 */
	async executeSync(request: ForecastRequest): Promise<ForecastResponse> {
		return apiClient('/forecasting/execute-sync', {
			method: 'POST',
			body: request
		});
	},

	/**
	 * Simple forecast with default parameters
	 */
	async executeForecast(instrumentCode: TradingInstrument): Promise<ForecastResponse> {
		return apiClient(`/forecasting/execute/${instrumentCode}`, {
			method: 'POST'
		});
	},

	/**
	 * Batch forecast for multiple instruments
	 */
	async executeBatch(request: BatchForecastRequest): Promise<BatchForecastResponse> {
		const forecasts = await Promise.all(
			request.instruments.map((instrument) =>
				this.execute({
					instrumentCode: instrument,
					startDate: request.startDate,
					endDate: request.endDate,
					isCurrentDate: request.isCurrentDate,
					includeCalculationDetails: false
				})
			)
		);

		const successfulForecasts = forecasts.filter((f) => f.status === 'SUCCESS').length;
		const failedInstruments = forecasts
			.filter((f) => f.status === 'FAILED')
			.map((f) => f.instrumentCode);

		return {
			forecasts,
			successfulForecasts,
			failedInstruments,
			executionTime: 0 // Would be calculated server-side
		};
	},

	/**
	 * Get forecast service health
	 */
	async health(): Promise<ForecastHealthResponse> {
		return apiClient('/forecasting/health');
	},

	/**
	 * Get model statistics
	 */
	async getModelStatistics(): Promise<any> {
		return apiClient('/forecasting/models/statistics');
	},

	/**
	 * Reload ARIMA models from master data (admin)
	 */
	async reloadModels(): Promise<{ success: boolean; message: string }> {
		return apiClient('/forecasting/master-data/reload', {
			method: 'POST'
		});
	}
};

/**
 * Server-side Forecasting API
 */
export const createServerForecastingApi = (fetch: typeof globalThis.fetch) => {
	const client = createServerApiClient(fetch);

	return {
		async execute(request: ForecastRequest): Promise<ForecastResponse> {
			return client('/forecasting/execute', {
				method: 'POST',
				body: request
			});
		},

		async executeSync(request: ForecastRequest): Promise<ForecastResponse> {
			return client('/forecasting/execute-sync', {
				method: 'POST',
				body: request
			});
		},

		async executeForecast(instrumentCode: TradingInstrument): Promise<ForecastResponse> {
			return client(`/forecasting/execute/${instrumentCode}`, {
				method: 'POST'
			});
		},

		async health(): Promise<ForecastHealthResponse> {
			return client('/forecasting/health');
		},

		async getModelStatistics(): Promise<any> {
			return client('/forecasting/models/statistics');
		}
	};
};
