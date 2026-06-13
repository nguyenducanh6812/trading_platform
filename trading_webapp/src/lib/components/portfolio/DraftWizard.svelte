<script lang="ts">
	import { Check } from 'lucide-svelte';
	import { Button } from '$lib/components/ui';
	import WizardStep1 from './WizardStep1.svelte';
	import WizardStep2 from './WizardStep2.svelte';
	import WizardStep3 from './WizardStep3.svelte';
	import WizardStep4 from './WizardStep4.svelte';
	import type { Portfolio, CreatePortfolioRequest } from '$lib/types';
	import { enhance } from '$app/forms';
	import { draftStore } from '$lib/stores/draft';
	import { browser } from '$app/environment';
	import { PORTFOLIO_DEFAULTS, WIZARD_STEPS } from '$lib/constants/portfolio';
	import { createDraftData, inferHighestStepReached } from '$lib/utils/draftHelpers';

	let {
		portfolio,
		portfolioId,
		onClose,
		onSuccess
	}: {
		portfolio?: Portfolio;
		portfolioId?: number;
		onClose: () => void;
		onSuccess?: () => void;
	} = $props();

	// Wizard state
	let currentStep = $state(1);
	let highestStepReached = $state(1); // Track the furthest step user has reached
	let lastSavedAt = $state<Date | null>(null);
	let isInitializing = $state(true);
	let hasLoadedDraft = $state(false);
	let saveTimeoutId: ReturnType<typeof setTimeout> | null = null;

	let formData = $state<Partial<CreatePortfolioRequest>>({
		name: '',
		description: '',
		initialCapital: PORTFOLIO_DEFAULTS.INITIAL_CAPITAL,
		currency: PORTFOLIO_DEFAULTS.CURRENCY,
		strategyType: PORTFOLIO_DEFAULTS.STRATEGY_TYPE,
		riskTolerance: PORTFOLIO_DEFAULTS.RISK_TOLERANCE,
		rebalancingFrequency: PORTFOLIO_DEFAULTS.REBALANCING_FREQUENCY,
		autoRebalance: PORTFOLIO_DEFAULTS.AUTO_REBALANCE,
		leverageRatio: PORTFOLIO_DEFAULTS.LEVERAGE_RATIO,
		leverageEnabled: PORTFOLIO_DEFAULTS.LEVERAGE_ENABLED,
		positions: [] // Selected instruments from Step 3
	});

	// Load draft on mount (runs once)
	// Only auto-load drafts when editing an existing portfolio (portfolioId is provided)
	// For new portfolios, start fresh to avoid confusing users with old draft data
	$effect(() => {
		if (browser && !hasLoadedDraft) {
			// Only load draft if we're editing an existing portfolio
			if (portfolioId) {
				const savedDraft = draftStore.load(portfolioId);
				if (savedDraft) {
					console.log('[DraftWizard] Loading saved draft:', savedDraft);
					// Set formData directly without spreading the old formData
					// This prevents circular dependency
					Object.assign(formData, savedDraft);

					// Infer highest step reached from saved data
					highestStepReached = inferHighestStepReached(savedDraft);
				}
			} else {
				// Clear any old drafts when starting a fresh portfolio creation
				console.log('[DraftWizard] Starting fresh - clearing old drafts');
				draftStore.clear();
			}
			hasLoadedDraft = true;

			// Allow auto-save after initial load
			setTimeout(() => {
				isInitializing = false;
			}, 100);
		}
	});

	// Initialize from portfolio if editing (overrides draft)
	$effect(() => {
		if (portfolio) {
			formData.name = portfolio.name || '';
			formData.description = portfolio.description || '';
			formData.initialCapital = portfolio.capital?.initial || PORTFOLIO_DEFAULTS.INITIAL_CAPITAL;
			formData.strategyType = portfolio.strategy?.type || PORTFOLIO_DEFAULTS.STRATEGY_TYPE;
			formData.riskTolerance = portfolio.strategy?.riskTolerance || PORTFOLIO_DEFAULTS.RISK_TOLERANCE;
			formData.rebalancingFrequency =
				portfolio.strategy?.rebalancingFrequency || PORTFOLIO_DEFAULTS.REBALANCING_FREQUENCY;
			formData.autoRebalance = portfolio.strategy?.autoRebalance ?? PORTFOLIO_DEFAULTS.AUTO_REBALANCE;
			formData.leverageRatio = portfolio.leverage?.ratio || PORTFOLIO_DEFAULTS.LEVERAGE_RATIO;
			formData.leverageEnabled = portfolio.leverage?.enabled || PORTFOLIO_DEFAULTS.LEVERAGE_ENABLED;

			// When editing existing portfolio, allow access to all steps
			highestStepReached = WIZARD_STEPS.TOTAL_STEPS;
		}
	});

	// Auto-save draft when formData changes (debounced)
	$effect(() => {
		if (browser && !isInitializing) {
			// Clear existing timeout
			if (saveTimeoutId) {
				clearTimeout(saveTimeoutId);
			}

			// Debounce save by 500ms
			saveTimeoutId = setTimeout(() => {
				const draftData = createDraftData(formData);
				draftStore.save(draftData, portfolioId);
				lastSavedAt = new Date();
				console.log('[DraftWizard] Auto-saved draft');
			}, 500);
		}
	});

	// Save draft on page unload
	$effect(() => {
		if (browser) {
			const handleBeforeUnload = () => {
				const draftData = createDraftData(formData);
				draftStore.save(draftData, portfolioId);
			};

			window.addEventListener('beforeunload', handleBeforeUnload);

			return () => {
				window.removeEventListener('beforeunload', handleBeforeUnload);
			};
		}
	});

	const steps = [
		{ number: WIZARD_STEPS.CORE_SETUP, title: 'Core Setup', description: 'Portfolio name and capital' },
		{ number: WIZARD_STEPS.STRATEGY, title: 'Strategy', description: 'Trading strategy and risk' },
		{ number: WIZARD_STEPS.ASSETS, title: 'Assets', description: 'Select trading instruments' },
		{ number: WIZARD_STEPS.BACKTEST, title: 'Backtest', description: 'Validate and deploy' }
	];

	// Navigation
	function nextStep() {
		if (currentStep < WIZARD_STEPS.TOTAL_STEPS) {
			currentStep++;
			// Track the highest step reached
			if (currentStep > highestStepReached) {
				highestStepReached = currentStep;
			}
		}
	}

	function prevStep() {
		if (currentStep > 1) {
			currentStep--;
		}
	}

	function goToStep(step: number) {
		// Allow navigating to any step up to the highest reached
		if (step <= highestStepReached) {
			currentStep = step;
		}
	}

	// Handlers
	function handleStep1Complete(data: any) {
		formData.name = data.name;
		formData.initialCapital = data.initialCapital;
		formData.leverageEnabled = data.leverageEnabled;
		formData.leverageRatio = data.leverageRatio;
		nextStep();
	}

	function handleStep2Complete(data: any) {
		formData.strategyType = data.strategyType;
		formData.riskTolerance = data.riskTolerance;
		formData.rebalancingFrequency = data.rebalancingFrequency;
		formData.autoRebalance = data.autoRebalance;
		nextStep();
	}

	function handleStep3Complete(data: any) {
		// Save selected positions (instruments)
		formData.positions = data.positions || [];
		console.log('[DraftWizard] Step 3 complete - Saved positions:', formData.positions?.length);
		nextStep();
	}

	async function handleStep4Complete() {
		// Clear draft on successful submission
		if (browser) {
			draftStore.clear(portfolioId);
			console.log('[DraftWizard] Cleared draft after successful submission');
		}

		// Form will be submitted via form action
		if (onSuccess) {
			onSuccess();
		}
	}

	// Enhanced close handler to save draft before closing
	function handleClose() {
		if (browser) {
			const draftData = createDraftData(formData);
			draftStore.save(draftData, portfolioId);
			console.log('[DraftWizard] Saved draft before closing');
		}
		onClose();
	}

	// Check if a step is actually completed (has valid data)
	function isStepCompleted(stepNumber: number): boolean {
		switch (stepNumber) {
			case WIZARD_STEPS.CORE_SETUP:
				// Step 1: Core Setup - needs name and initialCapital
				return !!(formData.name && formData.name.trim() !== '' && formData.initialCapital);
			case WIZARD_STEPS.STRATEGY:
				// Step 2: Strategy - needs strategyType selected
				return !!formData.strategyType;
			case WIZARD_STEPS.ASSETS:
				// Step 3: Assets - needs at least one position
				return !!(formData.positions && formData.positions.length > 0);
			case WIZARD_STEPS.BACKTEST:
				// Step 4: Backtest - never shows as completed (final step)
				return false;
			default:
				return false;
		}
	}
</script>

<div class="flex h-[600px]">
	<!-- Left - Step Progress -->
	<div class="w-64 bg-card border-r border-border p-6 flex flex-col">
		<h3 class="text-lg font-bold mb-6">{portfolio ? 'Edit Portfolio' : 'Create Portfolio'}</h3>

		<div class="space-y-4">
			{#each steps as step}
				{@const isCurrent = step.number === currentStep}
				{@const isCompleted = isStepCompleted(step.number) && !isCurrent}
				{@const isAccessible = step.number <= highestStepReached}
				{@const isDisabled = !isAccessible}
				<button
					onclick={() => goToStep(step.number)}
					disabled={isDisabled}
					class="w-full text-left flex items-start gap-3 p-3 rounded-lg transition-all {isCurrent
						? 'bg-accent/10 border border-accent'
						: isAccessible
							? 'hover:bg-background cursor-pointer border border-transparent'
							: 'opacity-50 cursor-not-allowed border border-transparent'}"
				>
					<div
						class="w-8 h-8 rounded-full flex items-center justify-center shrink-0 {isCurrent
							? 'bg-accent text-background'
							: isCompleted
								? 'bg-green-500 text-white'
								: isAccessible
									? 'bg-accent/20 text-accent'
									: 'bg-border text-gray-500'}"
					>
						{#if isCompleted}
							<Check size={16} />
						{:else}
							{step.number}
						{/if}
					</div>
					<div class="flex-1 min-w-0">
						<p class="font-bold text-sm text-white">{step.title}</p>
						<p class="text-xs text-gray-500 mt-0.5">{step.description}</p>
					</div>
				</button>
			{/each}
		</div>

		<!-- Auto-save Indicator -->
		<div class="mt-auto pt-4 border-t border-border">
			{#if lastSavedAt}
				<p class="text-xs text-gray-500">
					Auto-saved locally at {lastSavedAt.toLocaleTimeString()}
				</p>
			{/if}
		</div>
	</div>

	<!-- Right - Step Content -->
	<div class="flex-1 flex flex-col">
		<div class="flex-1 overflow-y-auto p-6">
			{#if currentStep === 1}
				<WizardStep1 data={formData} onComplete={handleStep1Complete} />
			{:else if currentStep === 2}
				<WizardStep2 data={formData} onComplete={handleStep2Complete} />
			{:else if currentStep === 3}
				<WizardStep3 data={formData} onComplete={handleStep3Complete} />
			{:else if currentStep === 4}
				<WizardStep4
					data={formData}
					{portfolio}
					onComplete={handleStep4Complete}
					onClose={handleClose}
				/>
			{/if}
		</div>

		<!-- Navigation Footer -->
		<div class="border-t border-border p-6 flex items-center justify-between bg-card">
			<div class="text-sm text-gray-400">
				Step {currentStep} of {steps.length}
			</div>
			<div class="flex gap-2">
				{#if currentStep > 1 && currentStep < 4}
					<Button variant="secondary" onclick={prevStep}>Back</Button>
				{/if}
				{#if currentStep === 1}
					<Button variant="secondary" onclick={handleClose}>Cancel</Button>
				{/if}
			</div>
		</div>
	</div>
</div>
