<script lang="ts">
	import { Button } from '$lib/components/ui';
	import { CheckCircle, AlertCircle, TrendingUp, Clock, Play } from 'lucide-svelte';
	import { enhance } from '$app/forms';
	import { toast } from '$lib/stores/toast';
	import type { CreatePortfolioRequest, Portfolio } from '$lib/types';

	let {
		data,
		portfolio,
		onComplete,
		onClose
	}: {
		data: Partial<CreatePortfolioRequest>;
		portfolio?: Portfolio;
		onComplete: () => void;
		onClose: () => void;
	} = $props();

	// State
	let backtestStatus = $state<'idle' | 'running' | 'success' | 'error'>('idle');
	let backtestResults = $state<any>(null);
	let isSubmitting = $state(false);
	let portfolioStatus = $state<'DRAFT' | 'ACTIVE'>('DRAFT');

	// Mock backtest (in real implementation, this would call the backtest API)
	async function runBacktest() {
		backtestStatus = 'running';

		// Simulate API call
		await new Promise((resolve) => setTimeout(resolve, 3000));

		// Mock results
		backtestResults = {
			totalReturn: 23.5,
			sharpeRatio: 1.45,
			maxDrawdown: -12.3,
			winRate: 58.2,
			trades: 142
		};

		backtestStatus = 'success';
	}

	// Format currency
	const formatCurrency = (value: number) =>
		'$' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
</script>

<div class="max-w-3xl mx-auto">
	<h2 class="text-2xl font-black mb-2">Backtest & Deploy</h2>
	<p class="text-gray-400 mb-8">
		Review your portfolio configuration and run a backtest before deployment
	</p>

	<div class="space-y-6">
		<!-- Configuration Summary -->
		<div class="bg-card border border-border rounded-xl p-6">
			<h4 class="font-bold text-white mb-4 flex items-center gap-2">
				<CheckCircle class="text-green-500" size={20} />
				Configuration Summary
			</h4>

			<div class="grid grid-cols-2 gap-4 text-sm">
				<div>
					<p class="text-gray-400 mb-1">Portfolio Name</p>
					<p class="font-bold text-white">{data.name || 'Unnamed Portfolio'}</p>
				</div>
				<div>
					<p class="text-gray-400 mb-1">Initial Capital</p>
					<p class="font-bold text-white">{formatCurrency(data.initialCapital || 0)}</p>
				</div>
				<div>
					<p class="text-gray-400 mb-1">Strategy</p>
					<p class="font-bold text-white">{data.strategyType?.replace(/_/g, ' ') || 'Not selected'}</p>
				</div>
				<div>
					<p class="text-gray-400 mb-1">Risk Tolerance</p>
					<p class="font-bold text-white">{data.riskTolerance || 'Not selected'}</p>
				</div>
				<div>
					<p class="text-gray-400 mb-1">Leverage</p>
					<p class="font-bold text-white">
						{data.leverageEnabled ? `${data.leverageRatio}x` : 'Disabled'}
					</p>
				</div>
				<div>
					<p class="text-gray-400 mb-1">Assets</p>
					<p class="font-bold text-white">{data.positions?.length || 0} Instruments</p>
				</div>
			</div>

			<!-- Assets List -->
			<div class="mt-4 pt-4 border-t border-border">
				<p class="text-sm text-gray-400 mb-2">Asset Allocation</p>
				<div class="flex flex-wrap gap-2">
					{#each data.positions || [] as position}
						<span
							class="inline-flex items-center px-3 py-1 rounded-lg bg-background border border-border"
						>
							<span class="font-bold text-white text-sm">{position.instrumentCode}</span>
							<span class="text-gray-500 text-xs ml-2"
								>{((position.weight || 0) * 100).toFixed(1)}%</span
							>
						</span>
					{/each}
				</div>
			</div>
		</div>

		<!-- Backtest Section -->
		<div class="bg-card border border-border rounded-xl p-6">
			<div class="flex items-center justify-between mb-4">
				<h4 class="font-bold text-white">Historical Backtest</h4>
				{#if backtestStatus === 'idle'}
					<Button size="sm" variant="secondary" onclick={runBacktest}>
						<Play size={16} />
						Run Backtest
					</Button>
				{/if}
			</div>

			{#if backtestStatus === 'idle'}
				<div class="text-center py-8">
					<Clock class="mx-auto text-gray-600 mb-3" size={48} />
					<p class="text-gray-500 text-sm">
						Test your strategy against historical data before going live
					</p>
				</div>
			{:else if backtestStatus === 'running'}
				<div class="text-center py-8">
					<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent mx-auto mb-3">
					</div>
					<p class="text-gray-400 text-sm">Running backtest simulation...</p>
				</div>
			{:else if backtestStatus === 'success' && backtestResults}
				<div class="grid grid-cols-2 md:grid-cols-3 gap-4">
					<div class="bg-background rounded-lg p-4">
						<p class="text-xs text-gray-500 mb-1">Total Return</p>
						<p
							class="text-xl font-black {backtestResults.totalReturn >= 0
								? 'text-green-500'
								: 'text-red-500'}"
						>
							{backtestResults.totalReturn >= 0 ? '+' : ''}{backtestResults.totalReturn}%
						</p>
					</div>
					<div class="bg-background rounded-lg p-4">
						<p class="text-xs text-gray-500 mb-1">Sharpe Ratio</p>
						<p class="text-xl font-black text-white">{backtestResults.sharpeRatio}</p>
					</div>
					<div class="bg-background rounded-lg p-4">
						<p class="text-xs text-gray-500 mb-1">Max Drawdown</p>
						<p class="text-xl font-black text-red-500">{backtestResults.maxDrawdown}%</p>
					</div>
					<div class="bg-background rounded-lg p-4">
						<p class="text-xs text-gray-500 mb-1">Win Rate</p>
						<p class="text-xl font-black text-white">{backtestResults.winRate}%</p>
					</div>
					<div class="bg-background rounded-lg p-4">
						<p class="text-xs text-gray-500 mb-1">Total Trades</p>
						<p class="text-xl font-black text-white">{backtestResults.trades}</p>
					</div>
					<div class="bg-background rounded-lg p-4 flex items-center justify-center">
						<CheckCircle class="text-green-500" size={32} />
					</div>
				</div>
			{/if}
		</div>

		<!-- Actions -->
		<form
			method="POST"
			action="?/{portfolio ? 'update' : 'create'}"
			use:enhance={() => {
				isSubmitting = true;
				return async ({ result, update }) => {
					await update({ reset: false });
					isSubmitting = false;

					// Check if submission failed
					if (result.type === 'failure') {
						// Show global error toast
						const errorMessage = result.data?.error || 'Failed to save portfolio';
						const errorDetails = result.data?.errorDetails || null;

						toast.error(errorMessage, errorDetails);

						// Log error details
						console.error('='.repeat(80));
						console.error('[Portfolio Submit] Form action failed');
						console.error('Error:', errorMessage);
						if (errorDetails) {
							console.error('Details:', errorDetails);
						}
						console.error('='.repeat(80));
					} else {
						// Success - show success toast and call onComplete
						toast.success(
							portfolio ? 'Portfolio updated successfully' : 'Portfolio created successfully'
						);
						onComplete();
					}

					// Always navigate back to portfolio page (success or failure)
					onClose();
				};
			}}
		>
			<!-- Hidden form fields -->
			<input type="hidden" name="name" value={data.name} />
			<input type="hidden" name="description" value={data.description || ''} />
			<input type="hidden" name="initialCapital" value={data.initialCapital} />
			<input type="hidden" name="currency" value={data.currency || 'USD'} />
			<input type="hidden" name="strategyType" value={data.strategyType} />
			<input type="hidden" name="riskTolerance" value={data.riskTolerance} />
			<input type="hidden" name="rebalancingFrequency" value={data.rebalancingFrequency} />
			<input type="hidden" name="autoRebalance" value={data.autoRebalance} />
			<input type="hidden" name="leverageRatio" value={data.leverageRatio} />
			<input type="hidden" name="leverageEnabled" value={data.leverageEnabled} />

			<!-- Symbols array (required by backend API) -->
			<input
				type="hidden"
				name="symbols"
				value={JSON.stringify(data.positions?.map((p) => p.instrumentCode) || [])}
			/>

			<!-- Portfolio status (DRAFT or ACTIVE) -->
			<input type="hidden" name="status" value={portfolioStatus} />

			{#if portfolio}
				<input type="hidden" name="id" value={portfolio.id} />
			{/if}

			<div class="flex gap-3">
				<Button
					type="submit"
					variant="secondary"
					fullWidth
					disabled={isSubmitting || !data.positions || data.positions.length === 0}
					onclick={(e) => {
						// Save as draft
						portfolioStatus = 'DRAFT';
					}}
				>
					{isSubmitting ? 'Saving...' : 'Save as Draft'}
				</Button>

				<Button
					type="submit"
					fullWidth
					disabled={backtestStatus !== 'success' || isSubmitting || !data.positions || data.positions.length === 0}
					onclick={(e) => {
						// Deploy as active portfolio
						portfolioStatus = 'ACTIVE';
					}}
				>
					{isSubmitting ? 'Deploying...' : 'Deploy Portfolio'}
				</Button>
			</div>

			{#if !data.positions || data.positions.length === 0}
				<div class="bg-red-500/10 border border-red-500 rounded-lg p-3 mt-4">
					<div class="flex items-start gap-2">
						<AlertCircle class="text-red-500 shrink-0" size={16} />
						<p class="text-xs text-red-500">
							Please select at least one instrument in Step 3 before saving
						</p>
					</div>
				</div>
			{:else if backtestStatus !== 'success'}
				<div class="bg-yellow-500/10 border border-yellow-500 rounded-lg p-3 mt-4">
					<div class="flex items-start gap-2">
						<AlertCircle class="text-yellow-500 shrink-0" size={16} />
						<p class="text-xs text-yellow-500">
							Run a backtest to validate your strategy before deployment
						</p>
					</div>
				</div>
			{/if}
		</form>
	</div>
</div>
