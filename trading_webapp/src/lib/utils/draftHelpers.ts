/**
 * Draft management utilities
 * Provides reusable functions for creating and managing portfolio drafts
 */

import type { CreatePortfolioRequest } from '$lib/types';

/**
 * Creates a serializable draft data object from formData
 * Used for localStorage persistence and beforeunload handlers
 *
 * @param formData - Partial portfolio data from the wizard
 * @returns Serializable draft object safe for localStorage
 *
 * @example
 * const draftData = createDraftData(formData);
 * draftStore.save(draftData, portfolioId);
 */
export function createDraftData(
	formData: Partial<CreatePortfolioRequest>
): Partial<CreatePortfolioRequest> {
	return {
		name: formData.name,
		description: formData.description,
		initialCapital: formData.initialCapital,
		currency: formData.currency,
		strategyType: formData.strategyType,
		riskTolerance: formData.riskTolerance,
		rebalancingFrequency: formData.rebalancingFrequency,
		autoRebalance: formData.autoRebalance,
		leverageRatio: formData.leverageRatio,
		leverageEnabled: formData.leverageEnabled,
		positions: formData.positions
	};
}

/**
 * Determines the highest wizard step reached based on draft data
 * Used to restore progress when loading a draft
 *
 * @param draft - Saved draft data
 * @returns The highest step number the user has completed (1-4)
 *
 * @example
 * const step = inferHighestStepReached(savedDraft);
 * highestStepReached = step; // 1, 2, or 3
 */
export function inferHighestStepReached(
	draft: Partial<CreatePortfolioRequest> | null
): number {
	if (!draft) return 1;

	// Check if user has selected assets (Step 3)
	if (draft.positions && draft.positions.length > 0) {
		return 3;
	}

	// Check if user has selected strategy (Step 2)
	if (draft.strategyType) {
		return 2;
	}

	// Default to Step 1
	return 1;
}
