<script lang="ts">
	import { Button, Card } from '$lib/components/ui';
	import { TrendingUp, Shield, Zap, Target } from 'lucide-svelte';
	import type { CreatePortfolioRequest, StrategyType, RiskTolerance } from '$lib/types';
	import { STRATEGY_TEMPLATES } from '$lib/types/strategy';

	let {
		data,
		onComplete
	}: {
		data: Partial<CreatePortfolioRequest>;
		onComplete: (data: any) => void;
	} = $props();

	// Local state - initialize from parent data
	let selectedStrategyType = $state<StrategyType>(data.strategyType || 'MPT');
	let selectedRiskTolerance = $state<RiskTolerance>(data.riskTolerance || 'MODERATE');
	let selectedRebalanceFrequency = $state(data.rebalancingFrequency || 'MONTHLY');
	let autoRebalance = $state(data.autoRebalance ?? true);

	// Track last parent values to detect external changes (e.g., draft loading)
	let lastParentStrategyType = $state<StrategyType>(data.strategyType || 'MPT');
	let lastParentRiskTolerance = $state<RiskTolerance>(data.riskTolerance || 'MODERATE');
	let lastParentRebalanceFrequency = $state(data.rebalancingFrequency || 'MONTHLY');
	let lastParentAutoRebalance = $state(data.autoRebalance ?? true);

	// Sync FROM parent when parent data changes externally (e.g., draft loading)
	$effect(() => {
		if (data.strategyType !== lastParentStrategyType) {
			selectedStrategyType = data.strategyType || 'MPT';
			lastParentStrategyType = data.strategyType || 'MPT';
		}
		if (data.riskTolerance !== lastParentRiskTolerance) {
			selectedRiskTolerance = data.riskTolerance || 'MODERATE';
			lastParentRiskTolerance = data.riskTolerance || 'MODERATE';
		}
		if (data.rebalancingFrequency !== lastParentRebalanceFrequency) {
			selectedRebalanceFrequency = data.rebalancingFrequency || 'MONTHLY';
			lastParentRebalanceFrequency = data.rebalancingFrequency || 'MONTHLY';
		}
		if (data.autoRebalance !== lastParentAutoRebalance) {
			autoRebalance = data.autoRebalance ?? true;
			lastParentAutoRebalance = data.autoRebalance ?? true;
		}
	});

	// Sync TO parent when local changes (runs before DOM updates)
	$effect.pre(() => {
		data.strategyType = selectedStrategyType;
		data.riskTolerance = selectedRiskTolerance;
		data.rebalancingFrequency = selectedRebalanceFrequency;
		data.autoRebalance = autoRebalance;
		// Update tracking
		lastParentStrategyType = selectedStrategyType;
		lastParentRiskTolerance = selectedRiskTolerance;
		lastParentRebalanceFrequency = selectedRebalanceFrequency;
		lastParentAutoRebalance = autoRebalance;
	});

	const strategyTypes: { value: StrategyType; label: string; description: string; icon: any }[] = [
		{
			value: 'MPT',
			label: 'Modern Portfolio Theory',
			description: 'Optimizes risk-adjusted returns using Sharpe ratio',
			icon: TrendingUp
		},
		{
			value: 'EQUAL_WEIGHT',
			label: 'Equal Weight',
			description: 'Allocates capital equally across all instruments',
			icon: Target
		},
		{
			value: 'MARKET_CAP_WEIGHT',
			label: 'Market Cap Weight',
			description: 'Allocates based on market capitalization',
			icon: Zap
		},
		{
			value: 'MOMENTUM',
			label: 'Momentum Strategy',
			description: 'Allocates based on price momentum and trends',
			icon: Zap
		}
	];

	const riskLevels: { value: RiskTolerance; label: string; description: string; color: string }[] =
		[
			{
				value: 'CONSERVATIVE',
				label: 'Conservative',
				description: 'Low risk, stable returns',
				color: 'green'
			},
			{
				value: 'MODERATE',
				label: 'Moderate',
				description: 'Balanced risk and reward',
				color: 'yellow'
			},
			{
				value: 'AGGRESSIVE',
				label: 'Aggressive',
				description: 'High risk, high potential returns',
				color: 'red'
			}
		];

	const rebalanceOptions = [
		{ value: 'HOURLY', label: 'Hourly' },
		{ value: 'DAILY', label: 'Daily' },
		{ value: 'WEEKLY', label: 'Weekly' }
	];

	// Get selected strategy template
	const selectedTemplate = $derived(
		STRATEGY_TEMPLATES.find((t) => t.type === selectedStrategyType)
	);

	function handleNext() {
		onComplete({
			strategyType: selectedStrategyType,
			riskTolerance: selectedRiskTolerance,
			rebalancingFrequency: selectedRebalanceFrequency,
			autoRebalance
		});
	}
</script>

<div class="max-w-2xl mx-auto">
	<h2 class="text-2xl font-black mb-2">Strategy Setup</h2>
	<p class="text-gray-400 mb-8">Choose your trading strategy and risk parameters</p>

	<div class="space-y-6">
		<!-- Strategy Type -->
		<div>
			<p class="block text-sm font-bold text-white mb-3">
				Trading Strategy <span class="text-red-500">*</span>
			</p>
			<div class="grid gap-3">
				{#each strategyTypes as strategy}
					{@const Icon = strategy.icon}
					<button
						onclick={() => (selectedStrategyType = strategy.value)}
						class="text-left p-4 rounded-xl border transition-all {selectedStrategyType ===
						strategy.value
							? 'bg-accent/10 border-accent'
							: 'bg-card border-border hover:border-accent/50'}"
					>
						<div class="flex items-start gap-3">
							<div
								class="w-10 h-10 rounded-lg {selectedStrategyType === strategy.value
									? 'bg-accent'
									: 'bg-accent/10'} flex items-center justify-center"
							>
								<Icon
									class={selectedStrategyType === strategy.value ? 'text-background' : 'text-accent'}
									size={20}
								/>
							</div>
							<div class="flex-1">
								<p class="font-bold text-white mb-1">{strategy.label}</p>
								<p class="text-sm text-gray-400">{strategy.description}</p>
							</div>
							{#if selectedStrategyType === strategy.value}
								<div class="w-5 h-5 rounded-full bg-accent flex items-center justify-center">
									<div class="w-2 h-2 rounded-full bg-background"></div>
								</div>
							{/if}
						</div>
					</button>
				{/each}
			</div>
		</div>

		<!-- Risk Tolerance -->
		<div>
			<p class="block text-sm font-bold text-white mb-3">
				Risk Tolerance <span class="text-red-500">*</span>
			</p>
			<div class="grid grid-cols-3 gap-3">
				{#each riskLevels as risk}
					<button
						onclick={() => (selectedRiskTolerance = risk.value)}
						class="p-4 rounded-xl border transition-all {selectedRiskTolerance === risk.value
							? `bg-${risk.color}-500/10 border-${risk.color}-500`
							: 'bg-card border-border hover:border-accent/50'}"
					>
						<div class="text-center">
							<Shield
								class={selectedRiskTolerance === risk.value
									? `text-${risk.color}-500`
									: 'text-gray-500'}
								size={24}
							/>
							<p class="font-bold text-white text-sm mt-2">{risk.label}</p>
							<p class="text-xs text-gray-500 mt-1">{risk.description}</p>
						</div>
					</button>
				{/each}
			</div>
		</div>

		<!-- Rebalance Frequency -->
		<div>
			<p class="block text-sm font-bold text-white mb-3">Rebalance Frequency</p>
			<div class="grid grid-cols-3 gap-3">
				{#each rebalanceOptions as option}
					<button
						onclick={() => (selectedRebalanceFrequency = option.value)}
						class="p-3 rounded-lg border transition-all {selectedRebalanceFrequency === option.value
							? 'bg-accent/10 border-accent'
							: 'bg-card border-border hover:border-accent/50'}"
					>
						<p class="font-bold text-white text-sm text-center">{option.label}</p>
					</button>
				{/each}
			</div>
			<p class="text-xs text-gray-500 mt-2">How often the strategy will rebalance positions</p>
		</div>

		<!-- Strategy Parameters Preview -->
		{#if selectedTemplate}
			<div class="bg-card border border-border rounded-xl p-6">
				<h4 class="font-bold text-white mb-3">Strategy Parameters</h4>
				<div class="space-y-2 text-sm">
					{#each Object.entries(selectedTemplate.parameters) as [key, value]}
						<div class="flex justify-between">
							<span class="text-gray-400 capitalize">{key.replace(/_/g, ' ')}:</span>
							<span class="font-medium text-white">{value}</span>
						</div>
					{/each}
				</div>
				<p class="text-xs text-gray-500 mt-3">{selectedTemplate.description}</p>
			</div>
		{/if}

		<!-- Next Button -->
		<div class="flex justify-end pt-4">
			<Button onclick={handleNext}>Next: Select Assets</Button>
		</div>
	</div>
</div>
