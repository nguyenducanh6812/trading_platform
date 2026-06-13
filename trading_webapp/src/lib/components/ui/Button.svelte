<script lang="ts">
	import type { Snippet } from 'svelte';

	type Variant = 'primary' | 'secondary' | 'danger' | 'ghost';
	type Size = 'sm' | 'md' | 'lg';

	let {
		variant = 'primary',
		size = 'md',
		disabled = false,
		loading = false,
		fullWidth = false,
		type = 'button',
		onclick,
		children
	}: {
		variant?: Variant;
		size?: Size;
		disabled?: boolean;
		loading?: boolean;
		fullWidth?: boolean;
		type?: 'button' | 'submit' | 'reset';
		onclick?: (event: MouseEvent) => void;
		children: Snippet;
	} = $props();

	const variants = {
		primary: 'bg-accent hover:bg-accent-hover text-background',
		secondary: 'bg-card border border-border hover:border-accent text-white',
		danger: 'bg-red-500/10 hover:bg-red-500/20 text-red-500 border border-red-500',
		ghost: 'bg-transparent hover:bg-white/5 text-white'
	};

	const sizes = {
		sm: 'py-1.5 px-3 text-sm',
		md: 'py-2 px-4 text-base',
		lg: 'py-3 px-6 text-lg'
	};

	const baseClasses =
		'font-bold rounded-lg transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed';
	const widthClass = $derived(fullWidth ? 'w-full' : '');
</script>

<button
	{type}
	class="{baseClasses} {variants[variant]} {sizes[size]} {widthClass}"
	disabled={disabled || loading}
	onclick={onclick}
>
	{#if loading}
		<div class="animate-spin rounded-full h-4 w-4 border-b-2 border-current"></div>
		<span>Loading...</span>
	{:else}
		{@render children()}
	{/if}
</button>

<style>
	button:active:not(:disabled) {
		transform: scale(0.98);
	}
</style>
