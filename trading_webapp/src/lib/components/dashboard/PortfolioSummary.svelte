<script lang="ts">
	import { Target, TrendingUp, TrendingDown, ArrowRight } from 'lucide-svelte';
	import { Card } from '$lib/components/ui';
	import type { Portfolio } from '$lib/types';

	let {
		portfolio,
		onclick
	}: {
		portfolio: Portfolio;
		onclick?: () => void;
	} = $props();

	const totalValue = $derived(portfolio.capital.current);
	const profitLoss = $derived(portfolio.capital.profitLoss);
	const profitLossPercentage = $derived(portfolio.capital.profitLossPercentage);
	const isPositive = $derived(profitLoss >= 0);
</script>

<button
	class="w-full text-left group"
	onclick={onclick}
	type="button"
>
	<div
		class="bg-card border border-border rounded-xl p-6 hover:border-accent transition-all relative overflow-hidden"
	>
		<!-- Hover indicator -->
		<div
			class="absolute right-0 top-0 p-2 opacity-0 group-hover:opacity-100 transition-opacity"
		>
			<ArrowRight class="text-accent" size={20} />
		</div>

		<div class="flex items-start justify-between gap-4">
			<!-- Left: Portfolio Info -->
			<div class="flex items-start gap-4 flex-1 min-w-0">
				<!-- Icon -->
				<div
					class="w-12 h-12 rounded-2xl bg-accent/10 border-2 border-accent flex items-center justify-center shrink-0"
				>
					<Target size={24} class="text-accent" />
				</div>

				<!-- Info -->
				<div class="flex-1 min-w-0">
					<h4 class="font-bold text-white truncate">{portfolio.name}</h4>
					<p class="text-xs text-gray-500 mt-0.5">
						{portfolio.positions.length} Assets • {portfolio.leverage.enabled
							? `${portfolio.leverage.ratio}x Leverage`
							: 'No Leverage'}
					</p>

					<!-- Strategy -->
					<div class="flex items-center gap-2 mt-2">
						<span
							class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-secondary/10 text-secondary"
						>
							{portfolio.strategy.type}
						</span>
						<span
							class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-500/10 text-gray-400"
						>
							{portfolio.strategy.riskTolerance}
						</span>
					</div>
				</div>
			</div>

			<!-- Right: Value & P/L -->
			<div class="text-right shrink-0">
				<p class="text-lg font-black text-white">
					${totalValue.toLocaleString('en-US', { minimumFractionDigits: 2 })}
				</p>

				<div class="flex items-center justify-end gap-1 mt-1">
					{#if isPositive}
						<TrendingUp size={14} class="text-green-500" />
					{:else}
						<TrendingDown size={14} class="text-red-500" />
					{/if}
					<p class="text-sm font-bold {isPositive ? 'text-green-500' : 'text-red-500'}">
						{isPositive ? '+' : ''}{profitLoss.toLocaleString('en-US', {
							minimumFractionDigits: 2
						})}
					</p>
				</div>

				<p class="text-xs {isPositive ? 'text-green-500' : 'text-red-500'} font-medium">
					{isPositive ? '+' : ''}{profitLossPercentage.toFixed(2)}%
				</p>
			</div>
		</div>

		<!-- Positions Preview -->
		{#if portfolio.positions.length > 0}
			<div class="mt-4 pt-4 border-t border-border">
				<div class="flex items-center gap-2 flex-wrap">
					{#each portfolio.positions.slice(0, 5) as position}
						<span
							class="inline-flex items-center px-2 py-1 rounded-lg text-xs font-medium bg-background border border-border"
						>
							{position.instrumentCode}
						</span>
					{/each}
					{#if portfolio.positions.length > 5}
						<span class="text-xs text-gray-500">+{portfolio.positions.length - 5} more</span>
					{/if}
				</div>
			</div>
		{/if}
	</div>
</button>
