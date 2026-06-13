// Global toast notification store
// Following best practices for centralized UI state management

import { writable } from 'svelte/store';

export interface ToastDetails {
	endpoint?: string;
	method?: string;
	statusCode?: number;
	errorType?: string;
	timestamp?: string;
	stack?: string;
	message?: string;
	response?: any;
}

export interface Toast {
	id: string;
	message: string;
	type: 'success' | 'error' | 'warning' | 'info';
	details?: ToastDetails;
	duration?: number; // Auto-dismiss after X milliseconds (0 = no auto-dismiss)
}

interface ToastStore {
	toasts: Toast[];
}

function createToastStore() {
	const { subscribe, update } = writable<ToastStore>({ toasts: [] });

	return {
		subscribe,

		/**
		 * Show a toast notification
		 */
		show: (toast: Omit<Toast, 'id'>) => {
			const id = crypto.randomUUID();
			const newToast: Toast = {
				id,
				duration: 5000, // Default 5 seconds
				...toast
			};

			update((state) => ({
				toasts: [...state.toasts, newToast]
			}));

			// Auto-dismiss after duration
			if (newToast.duration && newToast.duration > 0) {
				setTimeout(() => {
					toastStore.dismiss(id);
				}, newToast.duration);
			}

			return id;
		},

		/**
		 * Dismiss a specific toast
		 */
		dismiss: (id: string) => {
			update((state) => ({
				toasts: state.toasts.filter((t) => t.id !== id)
			}));
		},

		/**
		 * Clear all toasts
		 */
		clear: () => {
			update(() => ({ toasts: [] }));
		}
	};
}

export const toastStore = createToastStore();

// Convenience helpers for common toast types
export const toast = {
	success: (message: string, details?: ToastDetails) => {
		return toastStore.show({ message, type: 'success', details });
	},

	error: (message: string, details?: ToastDetails, duration = 0) => {
		// Errors don't auto-dismiss by default
		return toastStore.show({ message, type: 'error', details, duration });
	},

	warning: (message: string, details?: ToastDetails) => {
		return toastStore.show({ message, type: 'warning', details });
	},

	info: (message: string, details?: ToastDetails) => {
		return toastStore.show({ message, type: 'info', details });
	}
};
