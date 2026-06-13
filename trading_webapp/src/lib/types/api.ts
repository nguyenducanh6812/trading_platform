// General API types and utilities

export interface ApiResponse<T> {
	data: T;
	status: number;
	message?: string;
}

export interface ApiError {
	status: number;
	message: string;
	code?: string;
	details?: Record<string, any>;
	timestamp?: string;
}

export interface PaginationParams {
	page: number;
	size: number;
	sort?: string;
	order?: 'asc' | 'desc';
}

export interface PaginatedResponse<T> {
	content: T[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	last: boolean;
	first: boolean;
}

export interface HealthResponse {
	status: 'UP' | 'DOWN';
	timestamp: string;
	details?: Record<string, any>;
}

// Async operation status
export type AsyncStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface AsyncTaskResponse {
	taskId: string;
	status: AsyncStatus;
	progress?: number; // 0-100
	result?: any;
	error?: string;
	createdAt: string;
	completedAt?: string;
}

// Validation error
export interface ValidationError {
	field: string;
	message: string;
	rejectedValue?: any;
}

export interface ValidationErrorResponse {
	status: 400;
	message: string;
	errors: ValidationError[];
	timestamp: string;
}

// Generic success response
export interface SuccessResponse {
	success: boolean;
	message?: string;
	data?: any;
}

// User types
export interface User {
	id: string;
	username: string;
	email: string;
	createdAt?: string;
	roles?: string[];
}

// Date range filter
export interface DateRangeFilter {
	startDate: string; // YYYY-MM-DD
	endDate: string; // YYYY-MM-DD
}

// Sort options
export interface SortOption {
	field: string;
	direction: 'asc' | 'desc';
}
