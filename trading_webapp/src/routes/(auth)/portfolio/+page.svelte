<script lang="ts">
	import { Plus, Target, Edit, Trash2, Play, Pause } from 'lucide-svelte';
	import { Button, Card, Toast } from '$lib/components/ui';
	import { ConfirmationModal } from '$lib/components/layout';
	import { goto } from '$app/navigation';
	import type { PageData } from './$types';
	import type { Portfolio } from '$lib/types';

	let { data }: { data: PageData } = $props();

	// State
	let selectedPortfolio = $state<Portfolio | null>(null);
	let portfolioToDelete = $state<Portfolio | null>(null);
	let activeTab = $state<'active' | 'draft'>('active');
	let showError = $state(false);

	// Derive error state from data
	const hasError = $derived(!!data.error);

	// Update showError when data changes
	$effect(() => {
		if (hasError) {
			showError = true;
		}
	});

	// Filter portfolios (with null check)
	const activePortfolios = $derived(
		(data.portfolios || []).filter((p) => p.status === 'ACTIVE' || p.status === 'PAUSED')
	);
	const draftPortfolios = $derived((data.portfolios || []).filter((p) => p.status === 'DRAFT'));

	// Current list based on tab
	const currentList = $derived(activeTab === 'active' ? activePortfolios : draftPortfolios);

	// Format currency
	const formatCurrency = (value: number) =>
		'$' + value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

	// Handlers
	function handleSelectPortfolio(portfolio: Portfolio) {
		selectedPortfolio = portfolio;
	}

	function handleCreateNew() {
		goto('/portfolio/create');
	}

	function handleEdit(portfolio: Portfolio) {
		// Navigate to edit page (TODO: implement edit route)
		goto(`/portfolio/edit/${portfolio.id}`);
	}

	function handleDeleteClick(portfolio: Portfolio) {
		portfolioToDelete = portfolio;
	}

	async function handleDeleteConfirm() {
		if (!portfolioToDelete) return;

		// Create and submit form
		const form = document.createElement('form');
		form.method = 'POST';
		form.action = '?/delete';

		const input = document.createElement('input');
		input.type = 'hidden';
		input.name = 'id';
		input.value = portfolioToDelete.id.toString();
		form.appendChild(input);

		document.body.appendChild(form);
		form.submit();

		portfolioToDelete = null;
	}
</script>

<svelte:head>
	<title>Portfolio - Nexus Trade</title>
</svelte:head>

<div class="flex h-[calc(100vh-4rem)]">

	<!-- Left Sidebar - Portfolio List -->
	<div class="w-80 border-r border-border bg-background flex flex-col">
		<!-- Header -->
		<div class="p-4 border-b border-border">
			<div class="flex items-center justify-between mb-4">
				<h2 class="text-xl font-black">Portfolios</h2>
				<Button size="sm" onclick={handleCreateNew}>
					<Plus size={16} />
					New
				</Button>
			</div>

			<!-- Tabs -->
			<div class="flex gap-2">
				<button
					onclick={() => (activeTab = 'active')}
					class="flex-1 px-3 py-2 rounded-lg text-sm font-medium transition-all {activeTab ===
					'active'
						? 'bg-accent text-background'
						: 'bg-card text-gray-400 hover:text-white'}"
				>
					Active ({activePortfolios.length})
				</button>
				<button
					onclick={() => (activeTab = 'draft')}
					class="flex-1 px-3 py-2 rounded-lg text-sm font-medium transition-all {activeTab ===
					'draft'
						? 'bg-accent text-background'
						: 'bg-card text-gray-400 hover:text-white'}"
				>
					Drafts ({draftPortfolios.length})
				</button>
			</div>
		</div>

		<!-- Portfolio List -->
		<div class="flex-1 overflow-y-auto p-4 space-y-2">
			{#if currentList.length === 0}
				<div class="text-center py-12">
					<Target class="mx-auto text-gray-600 mb-3" size={48} />
					<p class="text-gray-500 text-sm">
						{activeTab === 'active' ? 'No active portfolios' : 'No draft portfolios'}
					</p>
				</div>
			{:else}
				{#each currentList as portfolio}
					<button
						onclick={() => handleSelectPortfolio(portfolio)}
						class="w-full text-left p-4 rounded-lg border transition-all {selectedPortfolio?.id ===
						portfolio.id
							? 'bg-accent/10 border-accent'
							: 'bg-card border-border hover:border-accent/50'}"
					>
						<div class="flex items-start justify-between gap-2 mb-2">
							<h4 class="font-bold text-white truncate">{portfolio.name}</h4>
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

						<p class="text-lg font-black text-white mb-1">
							{formatCurrency(portfolio.capital.current)}
						</p>

						<div class="flex items-center gap-2 text-xs text-gray-500">
							<span>{portfolio.positions.length} Assets</span>
							<span>•</span>
							<span>{portfolio.strategy.type}</span>
						</div>
					</button>
				{/each}
			{/if}
		</div>
	</div>

	<!-- Right Panel - Portfolio Details -->
	<div class="flex-1 overflow-y-auto p-6">
		{#if selectedPortfolio}
			<!-- Portfolio Details View -->
			<div>
				<div class="flex items-center justify-between mb-6">
					<div>
						<h2 class="text-2xl font-black mb-1">{selectedPortfolio.name}</h2>
						<p class="text-gray-400 text-sm">
							Created {new Date(selectedPortfolio.createdAt || '').toLocaleDateString()}
						</p>
					</div>
					<div class="flex gap-2">
						{#if selectedPortfolio.status === 'DRAFT'}
							<Button
								variant="secondary"
								size="sm"
								onclick={() => selectedPortfolio && handleEdit(selectedPortfolio)}
							>
								<Edit size={16} />
								Edit
							</Button>
						{/if}
						<Button
							variant="danger"
							size="sm"
							onclick={() => selectedPortfolio && handleDeleteClick(selectedPortfolio)}
						>
							<Trash2 size={16} />
							Delete
						</Button>
					</div>
				</div>

				<!-- TODO: Add ActivePortfolioView component here -->
				<Card padding="lg">
					<p class="text-gray-400">Portfolio details view coming soon...</p>
				</Card>
			</div>
		{:else}
			<!-- Empty State -->
			<div class="flex items-center justify-center h-full">
				<div class="text-center">
					<Target class="mx-auto text-gray-600 mb-4" size={64} />
					<h3 class="text-xl font-bold mb-2">Select a Portfolio</h3>
					<p class="text-gray-400 mb-6">
						Choose a portfolio from the list or create a new one to get started
					</p>
					<Button onclick={handleCreateNew}>
						<Plus size={16} />
						Create Portfolio
					</Button>
				</div>
			</div>
		{/if}
	</div>
</div>

<!-- Delete Confirmation Modal -->
{#if portfolioToDelete}
	<ConfirmationModal
		isOpen={!!portfolioToDelete}
		title="Delete Portfolio"
		message="Are you sure you want to delete '{portfolioToDelete.name}'? This action cannot be undone."
		confirmText="Delete"
		onConfirm={handleDeleteConfirm}
		onCancel={() => (portfolioToDelete = null)}
	/>
{/if}

<!-- Error Toast -->
{#if showError && data.error}
	<Toast
		message={data.error}
		details={data.errorDetails}
		type="error"
		onClose={() => (showError = false)}
	/>
{/if}
