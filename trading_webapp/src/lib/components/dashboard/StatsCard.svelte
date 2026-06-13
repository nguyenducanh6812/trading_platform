<script lang="ts">
	import type { ComponentType } from 'svelte';
	import type { Icon } from 'lucide-svelte';

	let {
		title,
		value,
		icon,
		iconColor = 'accent',
		change,
		loading = false
	}: {
		title: string;
		value: string | number;
		icon: ComponentType<Icon>;
		iconColor?: 'accent' | 'secondary' | 'success' | 'warning' | 'danger';
		change?: number;
		loading?: boolean;
	} = $props();

	const iconColors = {
		accent: 'bg-accent/10 text-accent',
		secondary: 'bg-secondary/10 text-secondary',
		success: 'bg-green-500/10 text-green-500',
		warning: 'bg-yellow-500/10 text-yellow-500',
		danger: 'bg-red-500/10 text-red-500'
	};

	const changeColor = $derived(
		change === undefined ? '' : change >= 0 ? 'text-green-500' : 'text-red-500'
	);

	const formattedValue = $derived(
		typeof value === 'number'
			? value.toLocaleString('en-US', {
					minimumFractionDigits: 0,
					maximumFractionDigits: 2
				})
			: value
	);
</script>

<div class="bg-card border border-border rounded-xl p-6 hover:border-accent/50 transition-all">
	<div class="flex items-center gap-4">
		<!-- Icon -->
		<div class="w-12 h-12 rounded-xl {iconColors[iconColor]} flex items-center justify-center">
			{#if loading}
				<div class="animate-spin rounded-full h-6 w-6 border-b-2 border-current"></div>
			{:else}
				{@const IconComponent = icon}
				<IconComponent size={24} />
			{/if}
		</div>

		<!-- Content -->
		<div class="flex-1 min-w-0">
			<p class="text-sm text-gray-400 font-bold uppercase tracking-wide">{title}</p>

			{#if loading}
				<div class="mt-1 h-8 w-24 bg-gray-700 rounded animate-pulse"></div>
			{:else}
				<div class="flex items-baseline gap-2">
					<p class="text-2xl font-black text-white truncate">{formattedValue}</p>
					{#if change !== undefined}
						<span class="text-sm font-bold {changeColor}">
							{change >= 0 ? '+' : ''}{change.toFixed(2)}%
						</span>
					{/if}
				</div>
			{/if}
		</div>
	</div>
</div>
