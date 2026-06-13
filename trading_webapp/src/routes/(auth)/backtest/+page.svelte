<script lang="ts">
	import {
		TrendingUp,
		TrendingDown,
		Calendar,
		Clock,
		Target,
		BarChart3,
		RefreshCw
	} from 'lucide-svelte';
	import { Button, Card } from '$lib/components/ui';
	import { invalidate } from '$app/navigation';
	import type { PageData } from './$types';
	import type { BacktestResult } from '$lib/types';

	let { data }: { data: PageData } = $props();

	let selectedBacktest = $state<BacktestResult | null>(null);
	let isRefreshing = $state(false);

	async function refreshData() {
		isRefreshing = true;
		await invalidate('app:backtest');
		isRefreshing = false;
	}

	// Format currency
	const formatCurrency = (value: number) =>
		'$' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

	// Format percentage
	const formatPercent = (value: number) => (value >= 0 ? '+' : '') + value.toFixed(2) + '%';

	// Format date
	const formatDate = (date: string) => new Date(date).toLocaleDateString();
</script>

<svelte:head>
	<title>Backtest History - Nexus Trade</title>
</svelte:head>

<div class="p-6">
	<div class="max-w-7xl mx-auto">
		<!-- Header -->
		<div class="flex items-center justify-between mb-8">
			<div>
				<h2 class="text-3xl font-black">Backtest History</h2>
				<p class="text-gray-400 mt-1">Historical strategy performance analysis</p>
			</div>
			<Button size="sm" variant="secondary" onclick={refreshData} disabled={isRefreshing}>
				<RefreshCw size={16} class={isRefreshing ? 'animate-spin' : ''} />
				Refresh
			</Button>
		</div>

		<!-- Error Alert -->
		{#if data.error}
			<div class="bg-yellow-500/10 border border-yellow-500 rounded-xl p-4 mb-6">
				<p class="text-yellow-500 font-medium">{data.error}</p>
			</div>
		{/if}

		<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
			<!-- Left - Backtest List -->
			<div class="lg:col-span-1">
				<Card padding="none">
					<div class="p-4 border-b border-border">
						<h3 class="font-bold text-white">Recent Backtests</h3>
						<p class="text-xs text-gray-500 mt-1">{data.backtests.length} total</p>
					</div>

					<div class="overflow-y-auto max-h-[600px]">
						{#if data.backtests.length === 0}
							<div class="p-8 text-center">
								<BarChart3 class="mx-auto text-gray-600 mb-3" size={48} />
								<p class="text-gray-500 text-sm">No backtests yet</p>
								<p class="text-xs text-gray-600 mt-1">Run a backtest from the Portfolio page</p>
							</div>
						{:else}
							<div class="divide-y divide-border">
								{#each data.backtests as backtest}
									{@const isPositive = backtest.totalReturn >= 0}
									<button
										onclick={() => (selectedBacktest = backtest)}
										class="w-full text-left p-4 hover:bg-accent/5 transition-all {selectedBacktest?.id ===
										backtest.id
											? 'bg-accent/10'
											: ''}"
									>
										<div class="flex items-start justify-between gap-2 mb-2">
											<div class="flex-1 min-w-0">
												<p class="font-bold text-white text-sm truncate">
													{backtest.portfolioName || `Backtest #${backtest.id}`}
												</p>
												<p class="text-xs text-gray-500 mt-1">
													{backtest.strategyType?.replace(/_/g, ' ')}
												</p>
											</div>
											<span
												class="text-sm font-bold {isPositive ? 'text-green-500' : 'text-red-500'}"
											>
												{formatPercent(backtest.totalReturn)}
											</span>
										</div>

										<div class="flex items-center gap-4 text-xs text-gray-500">
											<div class="flex items-center gap-1">
												<Calendar size={12} />
												{formatDate(backtest.startDate)}
											</div>
											<div class="flex items-center gap-1">
												<Target size={12} />
												{backtest.totalTrades} trades
											</div>
										</div>
									</button>
								{/each}
							</div>
						{/if}
					</div>
				</Card>
			</div>

			<!-- Right - Backtest Details -->
			<div class="lg:col-span-2">
				{#if selectedBacktest}
					<div class="space-y-6">
						<!-- Header -->
						<Card padding="lg">
							<div class="flex items-start justify-between mb-6">
								<div>
									<h3 class="text-2xl font-black mb-1">
										{selectedBacktest.portfolioName || `Backtest #${selectedBacktest.id}`}
									</h3>
									<p class="text-gray-400 text-sm">
										{selectedBacktest.strategyType?.replace(/_/g, ' ')} Strategy
									</p>
								</div>
								<span
									class="text-3xl font-black {selectedBacktest.totalReturn >= 0
										? 'text-green-500'
										: 'text-red-500'}"
								>
									{formatPercent(selectedBacktest.totalReturn)}
								</span>
							</div>

							<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
								<div>
									<p class="text-xs text-gray-500 mb-1">Period</p>
									<p class="font-bold text-white text-sm">
										{formatDate(selectedBacktest.startDate)} - {formatDate(
											selectedBacktest.endDate
										)}
									</p>
								</div>
								<div>
									<p class="text-xs text-gray-500 mb-1">Initial Capital</p>
									<p class="font-bold text-white text-sm">
										{formatCurrency(selectedBacktest.initialCapital)}
									</p>
								</div>
								<div>
									<p class="text-xs text-gray-500 mb-1">Final Value</p>
									<p class="font-bold text-white text-sm">
										{formatCurrency(selectedBacktest.finalValue)}
									</p>
								</div>
								<div>
									<p class="text-xs text-gray-500 mb-1">P&L</p>
									<p
										class="font-bold text-sm {selectedBacktest.totalReturn >= 0
											? 'text-green-500'
											: 'text-red-500'}"
									>
										{formatCurrency(
											selectedBacktest.finalValue - selectedBacktest.initialCapital
										)}
									</p>
								</div>
							</div>
						</Card>

						<!-- Performance Metrics -->
						<Card padding="lg">
							<h4 class="font-bold text-white mb-4">Performance Metrics</h4>
							<div class="grid grid-cols-2 md:grid-cols-3 gap-4">
								<div class="bg-background rounded-lg p-4">
									<p class="text-xs text-gray-500 mb-1">Sharpe Ratio</p>
									<p class="text-xl font-black text-white">{selectedBacktest.sharpeRatio}</p>
								</div>
								<div class="bg-background rounded-lg p-4">
									<p class="text-xs text-gray-500 mb-1">Max Drawdown</p>
									<p class="text-xl font-black text-red-500">
										{selectedBacktest.maxDrawdown}%
									</p>
								</div>
								<div class="bg-background rounded-lg p-4">
									<p class="text-xs text-gray-500 mb-1">Win Rate</p>
									<p class="text-xl font-black text-white">{selectedBacktest.winRate}%</p>
								</div>
								<div class="bg-background rounded-lg p-4">
									<p class="text-xs text-gray-500 mb-1">Total Trades</p>
									<p class="text-xl font-black text-white">{selectedBacktest.totalTrades}</p>
								</div>
								<div class="bg-background rounded-lg p-4">
									<p class="text-xs text-gray-500 mb-1">Winning Trades</p>
									<p class="text-xl font-black text-green-500">
										{selectedBacktest.winningTrades}
									</p>
								</div>
								<div class="bg-background rounded-lg p-4">
									<p class="text-xs text-gray-500 mb-1">Losing Trades</p>
									<p class="text-xl font-black text-red-500">{selectedBacktest.losingTrades}</p>
								</div>
							</div>
						</Card>

						<!-- Equity Curve -->
						<Card padding="lg">
							<h4 class="font-bold text-white mb-4">Equity Curve</h4>
							{#if selectedBacktest.equityCurve && selectedBacktest.equityCurve.length > 0}
								<div class="h-64 bg-background rounded-lg p-4">
									<!-- TODO: Add equity curve chart using LayerCake -->
									<div class="flex items-center justify-center h-full">
										<p class="text-gray-500 text-sm">Chart visualization coming soon</p>
									</div>
								</div>
							{:else}
								<div class="text-center py-8 text-gray-500 text-sm">
									No equity curve data available
								</div>
							{/if}
						</Card>

						<!-- Recent Trades -->
						<Card padding="lg">
							<h4 class="font-bold text-white mb-4">Recent Trades</h4>
							{#if selectedBacktest.trades && selectedBacktest.trades.length > 0}
								<div class="overflow-x-auto">
									<table class="w-full text-sm">
										<thead class="border-b border-border">
											<tr class="text-gray-400 text-xs">
												<th class="text-left py-2">Date</th>
												<th class="text-left py-2">Type</th>
												<th class="text-left py-2">Instrument</th>
												<th class="text-right py-2">Quantity</th>
												<th class="text-right py-2">Price</th>
												<th class="text-right py-2">P&L</th>
											</tr>
										</thead>
										<tbody class="divide-y divide-border">
											{#each selectedBacktest.trades.slice(0, 10) as trade}
												<tr>
													<td class="py-2 text-gray-400">{formatDate(trade.timestamp)}</td>
													<td class="py-2">
														<span
															class="px-2 py-0.5 rounded text-xs font-medium {trade.type ===
															'BUY'
																? 'bg-green-500/10 text-green-500'
																: 'bg-red-500/10 text-red-500'}"
														>
															{trade.type}
														</span>
													</td>
													<td class="py-2 font-medium text-white">{trade.instrumentCode}</td>
													<td class="py-2 text-right text-white">{trade.quantity}</td>
													<td class="py-2 text-right text-white"
														>{formatCurrency(trade.price)}</td
													>
													<td
														class="py-2 text-right font-bold {trade.profitLoss >= 0
															? 'text-green-500'
															: 'text-red-500'}"
													>
														{formatCurrency(trade.profitLoss)}
													</td>
												</tr>
											{/each}
										</tbody>
									</table>
								</div>
							{:else}
								<div class="text-center py-8 text-gray-500 text-sm">No trade data available</div>
							{/if}
						</Card>
					</div>
				{:else}
					<!-- Empty State -->
					<div class="flex items-center justify-center h-full">
						<div class="text-center">
							<BarChart3 class="mx-auto text-gray-600 mb-4" size={64} />
							<h3 class="text-xl font-bold mb-2">Select a Backtest</h3>
							<p class="text-gray-400">
								Choose a backtest from the list to view detailed results
							</p>
						</div>
					</div>
				{/if}
			</div>
		</div>
	</div>
</div>
