<script lang="ts">
	import { onMount } from 'svelte';
	import { Target, TrendingUp, TrendingDown, Activity, DollarSign } from 'lucide-svelte';
	import { Button, Card, Toast } from '$lib/components/ui';
	import { goto } from '$app/navigation';
	import { apiClient } from '$lib/api';
	import type { PageData } from './$types';
	import type { Portfolio } from '$lib/types';

	let { data }: { data: PageData } = $props();

	// State
	let portfolios = $state<Portfolio[]>([]);
	let isLoading = $state(true);
	let showError = $state(false);
	let errorMessage = $state('');
	let errorDetails = $state<any>(null);

	// Computed stats
	const stats = $derived({
		totalValue: portfolios.reduce((sum, p) => sum + (p.capital?.current || 0), 0),
		totalProfitLoss: portfolios.reduce((sum, p) => sum + (p.capital?.profitLoss || 0), 0),
		activePortfolios: portfolios.filter((p) => p.status === 'ACTIVE').length,
		totalPortfolios: portfolios.length
	});

	// Fetch data on mount
	onMount(async () => {
		try {
			console.log('[Dashboard] Fetching portfolios...');
			const response = await apiClient<Portfolio[]>(
				`/portfolios?userId=${data.user?.id || 'dev-user-123'}`
			);
			portfolios = response;
			console.log(`[Dashboard] Loaded ${portfolios.length} portfolios`);
		} catch (error: any) {
			console.error('='.repeat(80));
			console.error('[Dashboard] Failed to fetch portfolios');
			console.error('Error:', error.message);
			console.error('Full error:', error);
			console.error('='.repeat(80));

			errorMessage = error.message || 'Failed to load dashboard data. Backend may be offline.';

			// Extract error details for Toast
			errorDetails = {
				endpoint: `/portfolios?userId=${data.user?.id || 'dev-user-123'}`,
				method: 'GET',
				errorType: error.name || 'FETCH_ERROR',
				statusCode: error.status || 0,
				timestamp: new Date().toISOString(),
				message: error.message,
				response: error.response, // Backend response data
				stack: error.stack?.split('\n').slice(0, 8).join('\n')
			};

			showError = true;
		} finally {
			isLoading = false;
		}
	});

	// Format currency
	const formatCurrency = (value: number) =>
		'$' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

	// Format percentage
	const formatPercent = (value: number) => {
		const sign = value >= 0 ? '+' : '';
		return `${sign}${value.toFixed(2)}%`;
	};

	// Calculate profit/loss percentage
	const profitLossPercent = $derived(() => {
		if (stats.totalValue === 0) return 0;
		const initialValue = stats.totalValue - stats.totalProfitLoss;
		if (initialValue === 0) return 0;
		return (stats.totalProfitLoss / initialValue) * 100;
	});

	// Handlers
	function handleCreateNew() {
		goto('/portfolio/create');
	}

	function handleViewPortfolio(portfolioId: number) {
		goto(`/portfolio/edit/${portfolioId}`);
	}
</script>

<svelte:head>
	<title>Dashboard - Nexus Trade</title>
</svelte:head>

<div class="p-6 space-y-6">
	<!-- Header -->
	<div class="flex items-center justify-between">
		<div>
			<h1 class="text-3xl font-black">Dashboard</h1>
			<p class="text-gray-400 mt-1">Welcome back, {data.user?.username || 'User'}</p>
		</div>
		<Button onclick={handleCreateNew}>
			<Target size={16} />
			Create Portfolio
		</Button>
	</div>

	<!-- Stats Cards -->
	<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
		<!-- Total Value -->
		<Card padding="md">
			<div class="flex items-start justify-between">
				<div class="flex-1">
					<p class="text-sm text-gray-400 mb-1">Total Value</p>
					{#if isLoading}
						<div class="h-8 w-32 bg-gray-700 animate-pulse rounded"></div>
					{:else}
						<p class="text-2xl font-black text-white">{formatCurrency(stats.totalValue)}</p>
					{/if}
				</div>
				<div class="p-2 bg-accent/10 rounded-lg">
					<DollarSign class="text-accent" size={20} />
				</div>
			</div>
		</Card>

		<!-- Profit/Loss -->
		<Card padding="md">
			<div class="flex items-start justify-between">
				<div class="flex-1">
					<p class="text-sm text-gray-400 mb-1">Profit/Loss</p>
					{#if isLoading}
						<div class="h-8 w-32 bg-gray-700 animate-pulse rounded mb-1"></div>
						<div class="h-4 w-16 bg-gray-700 animate-pulse rounded"></div>
					{:else}
						<p
							class="text-2xl font-black {stats.totalProfitLoss >= 0
								? 'text-green-500'
								: 'text-red-500'}"
						>
							{formatCurrency(stats.totalProfitLoss)}
						</p>
						<p class="text-xs {stats.totalProfitLoss >= 0 ? 'text-green-500' : 'text-red-500'}">
							{formatPercent(profitLossPercent())}
						</p>
					{/if}
				</div>
				<div class="p-2 bg-accent/10 rounded-lg">
					{#if stats.totalProfitLoss >= 0}
						<TrendingUp class="text-green-500" size={20} />
					{:else}
						<TrendingDown class="text-red-500" size={20} />
					{/if}
				</div>
			</div>
		</Card>

		<!-- Active Portfolios -->
		<Card padding="md">
			<div class="flex items-start justify-between">
				<div class="flex-1">
					<p class="text-sm text-gray-400 mb-1">Active Portfolios</p>
					{#if isLoading}
						<div class="h-8 w-16 bg-gray-700 animate-pulse rounded"></div>
					{:else}
						<p class="text-2xl font-black text-white">{stats.activePortfolios}</p>
					{/if}
				</div>
				<div class="p-2 bg-accent/10 rounded-lg">
					<Activity class="text-accent" size={20} />
				</div>
			</div>
		</Card>

		<!-- Total Portfolios -->
		<Card padding="md">
			<div class="flex items-start justify-between">
				<div class="flex-1">
					<p class="text-sm text-gray-400 mb-1">Total Portfolios</p>
					{#if isLoading}
						<div class="h-8 w-16 bg-gray-700 animate-pulse rounded"></div>
					{:else}
						<p class="text-2xl font-black text-white">{stats.totalPortfolios}</p>
					{/if}
				</div>
				<div class="p-2 bg-accent/10 rounded-lg">
					<Target class="text-accent" size={20} />
				</div>
			</div>
		</Card>
	</div>

	<!-- Recent Portfolios -->
	<Card title="Recent Portfolios" padding="none">
		{#if isLoading}
			<!-- Loading Skeletons -->
			<div class="divide-y divide-border">
				{#each Array(3) as _, i}
					<div class="p-4">
						<div class="flex items-center justify-between">
							<div class="flex-1">
								<div class="flex items-center gap-3 mb-2">
									<div class="h-6 w-40 bg-gray-700 animate-pulse rounded"></div>
									<div class="h-5 w-16 bg-gray-700 animate-pulse rounded"></div>
								</div>
								<div class="flex items-center gap-4">
									<div class="h-4 w-24 bg-gray-700 animate-pulse rounded"></div>
									<div class="h-4 w-16 bg-gray-700 animate-pulse rounded"></div>
								</div>
							</div>
							<div class="text-right">
								<div class="h-6 w-28 bg-gray-700 animate-pulse rounded mb-1"></div>
								<div class="h-4 w-20 bg-gray-700 animate-pulse rounded"></div>
							</div>
						</div>
					</div>
				{/each}
			</div>
		{:else if portfolios.length === 0}
			<div class="text-center py-12">
				<Target class="mx-auto text-gray-600 mb-3" size={48} />
				<p class="text-gray-500 text-sm mb-4">No portfolios yet</p>
				<Button onclick={handleCreateNew}>
					<Target size={16} />
					Create Your First Portfolio
				</Button>
			</div>
		{:else}
			<div class="divide-y divide-border">
				{#each portfolios.slice(0, 5) as portfolio}
					<button
						onclick={() => handleViewPortfolio(portfolio.id)}
						class="w-full text-left p-4 hover:bg-background transition-colors"
					>
						<div class="flex items-center justify-between">
							<div class="flex-1">
								<div class="flex items-center gap-3 mb-2">
									<h4 class="font-bold text-white">{portfolio.name}</h4>
									{#if portfolio.status === 'ACTIVE'}
										<span
											class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-500/10 text-green-500"
										>
											Active
										</span>
									{:else if portfolio.status === 'PAUSED'}
										<span
											class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-yellow-500/10 text-yellow-500"
										>
											Paused
										</span>
									{:else}
										<span
											class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-500/10 text-gray-400"
										>
											Draft
										</span>
									{/if}
								</div>
								<div class="flex items-center gap-4 text-sm text-gray-400">
									<span>{portfolio.strategy?.type?.replace(/_/g, ' ')}</span>
									<span>•</span>
									<span>{portfolio.positions?.length || 0} Assets</span>
								</div>
							</div>
							<div class="text-right">
								<p class="text-lg font-black text-white">
									{formatCurrency(portfolio.capital?.current || 0)}
								</p>
								<p
									class="text-sm {(portfolio.capital?.profitLoss || 0) >= 0
										? 'text-green-500'
										: 'text-red-500'}"
								>
									{formatCurrency(portfolio.capital?.profitLoss || 0)}
								</p>
							</div>
						</div>
					</button>
				{/each}
			</div>
		{/if}
	</Card>
</div>

<!-- Error Toast -->
{#if showError}
	<Toast
		message={errorMessage}
		details={errorDetails}
		type="error"
		onClose={() => (showError = false)}
	/>
{/if}
