<script lang="ts">
	import type { Snippet } from 'svelte';

	type Padding = 'none' | 'sm' | 'md' | 'lg';

	let {
		title,
		subtitle,
		padding = 'md',
		hover = false,
		clickable = false,
		onclick,
		children,
		header,
		footer
	}: {
		title?: string;
		subtitle?: string;
		padding?: Padding;
		hover?: boolean;
		clickable?: boolean;
		onclick?: (event: MouseEvent) => void;
		children: Snippet;
		header?: Snippet;
		footer?: Snippet;
	} = $props();

	const paddingClasses = {
		none: 'p-0',
		sm: 'p-4',
		md: 'p-6',
		lg: 'p-8'
	};

	const baseClasses = 'bg-card border border-border rounded-xl shadow-lg';
	const hoverClasses = $derived(hover ? 'hover:border-accent transition-all' : '');
	const clickableClasses = $derived(clickable ? 'cursor-pointer' : '');
</script>

{#if clickable}
<div
	class="{baseClasses} {hoverClasses} {clickableClasses}"
	role="button"
	tabindex="0"
	onclick={onclick}
	onkeydown={(e) => {
		if (onclick && (e.key === 'Enter' || e.key === ' ')) {
			e.preventDefault();
			onclick(e as any);
		}
	}}
>
	{#if title || subtitle || header}
		<div class="border-b border-border {paddingClasses[padding]}">
			{#if header}
				{@render header()}
			{:else if title || subtitle}
				<div>
					{#if title}
						<h3 class="text-lg font-bold text-white">{title}</h3>
					{/if}
					{#if subtitle}
						<p class="text-sm text-gray-400 mt-1">{subtitle}</p>
					{/if}
				</div>
			{/if}
		</div>
	{/if}

	<div class={paddingClasses[padding]}>
		{@render children()}
	</div>

	{#if footer}
		<div class="border-t border-border {paddingClasses[padding]}">
			{@render footer()}
		</div>
	{/if}
</div>
{:else}
<div class="{baseClasses} {hoverClasses} {clickableClasses}">
	{#if title || subtitle || header}
		<div class="border-b border-border {paddingClasses[padding]}">
			{#if header}
				{@render header()}
			{:else if title || subtitle}
				<div>
					{#if title}
						<h3 class="text-lg font-bold text-white">{title}</h3>
					{/if}
					{#if subtitle}
						<p class="text-sm text-gray-400 mt-1">{subtitle}</p>
					{/if}
				</div>
			{/if}
		</div>
	{/if}

	<div class={paddingClasses[padding]}>
		{@render children()}
	</div>

	{#if footer}
		<div class="border-t border-border {paddingClasses[padding]}">
			{@render footer()}
		</div>
	{/if}
</div>
{/if}
