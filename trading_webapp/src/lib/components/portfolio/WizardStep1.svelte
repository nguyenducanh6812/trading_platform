<script lang="ts">
	import { Input, Button } from '$lib/components/ui';
	import { DollarSign, TrendingUp } from 'lucide-svelte';
	import type { CreatePortfolioRequest } from '$lib/types';

	let {
		data,
		onComplete
	}: {
		data: Partial<CreatePortfolioRequest>;
		onComplete: (data: any) => void;
	} = $props();

	// Local state - use two-way binding with parent data
	let name = $state(data.name || '');
	let initialCapital = $state(data.initialCapital || 10000);
	let leverageEnabled = $state(data.leverageEnabled || false);
	let leverageRatio = $state(data.leverageRatio || 1);
	let maxExposure = $state(10000);

	// Track last parent values to detect external changes (e.g., draft loading)
	let lastParentName = $state(data.name || '');
	let lastParentCapital = $state(data.initialCapital || 10000);
	let lastParentLeverageEnabled = $state(data.leverageEnabled || false);
	let lastParentLeverageRatio = $state(data.leverageRatio || 1);

	// Sync FROM parent when parent data changes externally (e.g., draft loading)
	$effect(() => {
		if (data.name !== lastParentName) {
			name = data.name || '';
			lastParentName = data.name || '';
		}
		if (data.initialCapital !== lastParentCapital) {
			initialCapital = data.initialCapital || 10000;
			lastParentCapital = data.initialCapital || 10000;
		}
		if (data.leverageEnabled !== lastParentLeverageEnabled) {
			leverageEnabled = data.leverageEnabled || false;
			lastParentLeverageEnabled = data.leverageEnabled || false;
		}
		if (data.leverageRatio !== lastParentLeverageRatio) {
			leverageRatio = data.leverageRatio || 1;
			lastParentLeverageRatio = data.leverageRatio || 1;
		}
	});

	// Sync TO parent when local changes (runs before DOM updates)
	$effect.pre(() => {
		data.name = name;
		data.initialCapital = initialCapital;
		data.leverageEnabled = leverageEnabled;
		data.leverageRatio = leverageRatio;
		// Update tracking
		lastParentName = name;
		lastParentCapital = initialCapital;
		lastParentLeverageEnabled = leverageEnabled;
		lastParentLeverageRatio = leverageRatio;
	});

	// Validation
	const isValid = $derived(
		name.trim().length > 0 && initialCapital > 0 && leverageRatio >= 1 && leverageRatio <= 10
	);

	// Calculate max exposure when leverage changes
	$effect(() => {
		if (leverageEnabled) {
			maxExposure = initialCapital * leverageRatio;
		} else {
			maxExposure = initialCapital;
			leverageRatio = 1;
		}
	});

	function handleNext() {
		if (!isValid) return;

		onComplete({
			name,
			initialCapital,
			leverageEnabled,
			leverageRatio
		});
	}
</script>

<div class="max-w-2xl mx-auto">
	<h2 class="text-2xl font-black mb-2">Core Setup</h2>
	<p class="text-gray-400 mb-8">Set up your portfolio name and initial capital</p>

	<div class="space-y-6">
		<!-- Portfolio Name -->
		<div>
			<label for="name" class="block text-sm font-bold text-white mb-2">
				Portfolio Name <span class="text-red-500">*</span>
			</label>
			<Input
				id="name"
				bind:value={name}
				placeholder="My Crypto Portfolio"
				error={name.trim().length === 0 ? 'Portfolio name is required' : ''}
			/>
		</div>

		<!-- Initial Capital -->
		<div>
			<label for="capital" class="block text-sm font-bold text-white mb-2">
				Initial Capital <span class="text-red-500">*</span>
			</label>
			<Input
				id="capital"
				type="number"
				bind:value={initialCapital}
				min={100}
				step={100}
				placeholder="10000"
				icon={DollarSign}
			/>
			<p class="text-xs text-gray-500 mt-1">Minimum: $100</p>
		</div>

		<!-- Leverage Toggle -->
		<div class="bg-card border border-border rounded-xl p-6">
			<div class="flex items-start gap-4">
				<input
					type="checkbox"
					id="leverage"
					bind:checked={leverageEnabled}
					class="w-5 h-5 mt-0.5 rounded border-border bg-background text-accent focus:ring-accent focus:ring-offset-0"
				/>
				<div class="flex-1">
					<label for="leverage" class="block text-sm font-bold text-white mb-1 cursor-pointer">
						Enable Leverage
					</label>
					<p class="text-xs text-gray-500">
						Use leverage to increase your trading exposure. Higher risk, higher reward.
					</p>
				</div>
			</div>

			{#if leverageEnabled}
				<div class="mt-4 pt-4 border-t border-border space-y-4">
					<!-- Leverage Ratio -->
					<div>
						<label for="ratio" class="block text-sm font-bold text-white mb-2">
							Leverage Ratio
						</label>
						<div class="flex items-center gap-4">
							<input
								type="range"
								id="ratio"
								bind:value={leverageRatio}
								min={1}
								max={10}
								step={0.5}
								class="flex-1 h-2 rounded-lg appearance-none bg-border accent-accent"
							/>
							<span class="text-lg font-black text-accent w-16 text-right">{leverageRatio}x</span>
						</div>
						<p class="text-xs text-gray-500 mt-1">1x (No leverage) to 10x (Maximum leverage)</p>
					</div>

					<!-- Max Exposure -->
					<div class="bg-background rounded-lg p-4">
						<div class="flex items-center justify-between">
							<div>
								<p class="text-sm font-bold text-white mb-1">Maximum Exposure</p>
								<p class="text-xs text-gray-500">Total position size including leverage</p>
							</div>
							<div class="flex items-center gap-2">
								<TrendingUp class="text-accent" size={20} />
								<span class="text-xl font-black text-accent">
									${maxExposure.toLocaleString('en-US', { minimumFractionDigits: 2 })}
								</span>
							</div>
						</div>
					</div>
				</div>
			{/if}
		</div>

		<!-- Summary -->
		<div class="bg-accent/10 border border-accent rounded-xl p-6">
			<h4 class="font-bold text-white mb-3">Summary</h4>
			<div class="space-y-2 text-sm">
				<div class="flex justify-between">
					<span class="text-gray-400">Portfolio Name:</span>
					<span class="font-bold text-white">{name || '—'}</span>
				</div>
				<div class="flex justify-between">
					<span class="text-gray-400">Initial Capital:</span>
					<span class="font-bold text-white">
						${initialCapital.toLocaleString('en-US', { minimumFractionDigits: 2 })}
					</span>
				</div>
				<div class="flex justify-between">
					<span class="text-gray-400">Leverage:</span>
					<span class="font-bold text-white">
						{leverageEnabled ? `${leverageRatio}x` : 'Disabled'}
					</span>
				</div>
				<div class="flex justify-between">
					<span class="text-gray-400">Max Exposure:</span>
					<span class="font-bold text-accent">
						${maxExposure.toLocaleString('en-US', { minimumFractionDigits: 2 })}
					</span>
				</div>
			</div>
		</div>

		<!-- Next Button -->
		<div class="flex justify-end pt-4">
			<Button onclick={handleNext} disabled={!isValid}>Next: Strategy Setup</Button>
		</div>
	</div>
</div>
