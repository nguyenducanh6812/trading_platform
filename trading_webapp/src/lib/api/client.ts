// Base API client with error handling and interceptors

import { ofetch, type FetchOptions } from 'ofetch';
import { browser } from '$app/environment';

const API_BASE = browser
	? import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'
	: process.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export interface ApiError {
	status: number;
	message: string;
	code?: string;
	details?: Record<string, any>;
}

export class ApiClientError extends Error {
	constructor(
		public status: number,
		message: string,
		public code?: string,
		public details?: Record<string, any>,
		public response?: any
	) {
		super(message);
		this.name = 'ApiClientError';
	}
}

/**
 * Client-side API client (for use in browser)
 * Automatically includes credentials and handles errors
 */
export const apiClient = ofetch.create({
	baseURL: API_BASE,
	credentials: 'include', // Send cookies
	timeout: 5000, // 5 second timeout
	headers: {
		'Content-Type': 'application/json'
	},

	onRequest({ options }) {
		// Add CSRF token if available
		if (browser) {
			const csrfToken = document
				.querySelector('meta[name="csrf-token"]')
				?.getAttribute('content');
			if (csrfToken) {
				// @ts-ignore - ofetch type definitions are not perfect
				options.headers = options.headers || {};
				// @ts-ignore
				options.headers['X-CSRF-Token'] = csrfToken;
			}
		}
	},

	onResponseError({ response }) {
		console.error(`API Error:`, response.status, response._data);

		// Global error handling
		if (browser) {
			if (response.status === 401) {
				// Redirect to login on unauthorized
				window.location.href = '/login';
			} else if (response.status === 403) {
				console.error('Forbidden: You do not have permission to perform this action');
			} else if (response.status >= 500) {
				console.error('Server error: Please try again later');
			}
		}

		// Throw custom error with full response data
		const errorMessage = response._data?.message || response.statusText || 'An error occurred';
		throw new ApiClientError(
			response.status,
			errorMessage,
			response._data?.code,
			response._data?.details,
			response._data // Include full response body
		);
	}
});

/**
 * Server-side API client factory (for use in load functions and actions)
 * Uses SvelteKit's fetch for SSR compatibility
 */
export const createServerApiClient = (fetchFn: typeof globalThis.fetch) => {
	return ofetch.create({
		baseURL: API_BASE,
		timeout: 5000, // 5 second timeout
		// @ts-ignore - ofetch type definitions don't include fetch parameter
		fetch: fetchFn, // Use SvelteKit's fetch for SSR

		onResponseError({ response }) {
			console.error('Server API Error:', response.status, response._data);

			// Throw custom error with full response data
			const errorMessage = response._data?.message || response.statusText || 'An error occurred';
			throw new ApiClientError(
				response.status,
				errorMessage,
				response._data?.code,
				response._data?.details,
				response._data // Include full response body
			);
		}
	});
};

/**
 * Helper to handle API errors in components
 */
export function getErrorMessage(error: unknown): string {
	if (error instanceof ApiClientError) {
		return error.message;
	}
	if (error instanceof Error) {
		return error.message;
	}
	return 'An unexpected error occurred';
}

/**
 * Helper to check if error is a specific status code
 */
export function isApiError(error: unknown, status?: number): error is ApiClientError {
	if (error instanceof ApiClientError) {
		return status ? error.status === status : true;
	}
	return false;
}
