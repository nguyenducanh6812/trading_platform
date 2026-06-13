<script lang="ts">
	import { TrendingUp, TrendingDown, RefreshCw, Zap } from 'lucide-svelte';
	import { Button, Card } from '$lib/components/ui';
	import { invalidate } from '$app/navigation';
	import type { PageData } from './$types';
	import type { MarketInstrumentUI } from '$lib/types';

	let { data }: { data: PageData } = $props();

	let isRefreshing = $state(false);
	let selectedInstrument = $state<MarketInstrumentUI | null>(null);

	async function refreshData() {
		isRefreshing = true;
		await invalidate('app:markets');
		isRefreshing = false;
	}

	// Format currency
	const formatCurrency = (value: number) =>
		'$' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

	// Format percentage
	const formatPercent = (value: number) =>
		(value >= 0 ? '+' : '') + value.toFixed(2) + '%';

	// Handle quick strategy creation
	function handleQuickStrategy(instrument: MarketInstrument) {
		// Navigate to portfolio creation with pre-selected instrument
		window.location.href = `/portfolio?instrument=${instrument.symbol}`;
	}
</script>

<svelte:head>
	<title>Markets - Nexus Trade</title>
</svelte:head>

<div class="p-6">
	<div class="max-w-7xl mx-auto">
		<!-- Header -->
		<div class="flex items-center justify-between mb-8">
			<div>
				<h2 class="text-3xl font-black">Markets</h2>
				<p class="text-gray-400 mt-1">Real-time cryptocurrency market data</p>
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

		<!-- Markets Grid -->
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
			{#each data.instruments as instrument}
				{@const isPositive = instrument.priceChange24h >= 0}
				<div
					onclick={() => (selectedInstrument = instrument)}
					class="group text-left bg-card border border-border rounded-xl p-6 hover:border-accent transition-all relative overflow-hidden cursor-pointer"
					role="button"
					tabindex="0"
					onkeydown={(e) => {
						if (e.key === 'Enter' || e.key === ' ') {
							e.preventDefault();
							selectedInstrument = instrument;
						}
					}}
				>
					<!-- Background gradient on hover -->
					<div
						class="absolute inset-0 bg-gradient-to-br from-accent/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"
					></div>

					<div class="relative">
						<!-- Header -->
						<div class="flex items-start justify-between mb-4">
							<div>
								<h4 class="text-2xl font-black text-white">{instrument.symbol}</h4>
								<p class="text-sm text-gray-500">{instrument.name}</p>
							</div>
							<div
								class="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center text-xl"
							>
								{instrument.icon || '₿'}
							</div>
						</div>

						<!-- Price -->
						<div class="mb-4">
							<p class="text-3xl font-black text-white mb-1">
								{formatCurrency(instrument.currentPrice)}
							</p>
							<div class="flex items-center gap-2">
								{#if isPositive}
									<TrendingUp class="text-green-500" size={16} />
								{:else}
									<TrendingDown class="text-red-500" size={16} />
								{/if}
								<span
									class="text-sm font-bold {isPositive ? 'text-green-500' : 'text-red-500'}"
								>
									{formatPercent(instrument.priceChange24h)}
								</span>
								<span class="text-xs text-gray-500">24h</span>
							</div>
						</div>

						<!-- Stats -->
						<div class="grid grid-cols-2 gap-3 text-xs">
							<div>
								<p class="text-gray-500 mb-1">24h High</p>
								<p class="font-bold text-white">{formatCurrency(instrument.high24h)}</p>
							</div>
							<div>
								<p class="text-gray-500 mb-1">24h Low</p>
								<p class="font-bold text-white">{formatCurrency(instrument.low24h)}</p>
							</div>
							<div>
								<p class="text-gray-500 mb-1">Volume</p>
								<p class="font-bold text-white">
									${(instrument.volume24h / 1e9).toFixed(2)}B
								</p>
							</div>
							<div>
								<p class="text-gray-500 mb-1">Market Cap</p>
								<p class="font-bold text-white">
									${(instrument.marketCap / 1e9).toFixed(1)}B
								</p>
							</div>
						</div>

						<!-- Quick Action -->
						<div class="mt-4 pt-4 border-t border-border opacity-0 group-hover:opacity-100 transition-opacity">
							<button
								onclick={(e) => {
									e.stopPropagation();
									handleQuickStrategy(instrument);
								}}
								class="w-full px-3 py-2 bg-accent hover:bg-accent-hover text-background font-bold rounded-lg text-sm transition-all flex items-center justify-center gap-2"
							>
								<Zap size={14} />
								Quick Strategy
							</button>
						</div>
					</div>
				</div>
			{/each}
		</div>

		<!-- Empty State -->
		{#if data.instruments.length === 0 && !data.error}
			<div class="text-center py-12">
				<TrendingUp class="mx-auto text-gray-600 mb-4" size={64} />
				<h3 class="text-xl font-bold mb-2">No Market Data</h3>
				<p class="text-gray-400 mb-6">Market data is currently unavailable</p>
				<Button onclick={refreshData}>
					<RefreshCw size={16} />
					Refresh
				</Button>
			</div>
		{/if}
	</div>
</div>

<!-- Market Detail Modal (Optional - can be added later) -->
{#if selectedInstrument}
	<!-- TODO: Add detailed market view modal -->
{/if}
