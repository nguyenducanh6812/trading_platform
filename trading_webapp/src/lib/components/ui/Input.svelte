<script lang="ts">
	import type { Component } from 'svelte';

	type InputType = 'text' | 'email' | 'password' | 'number' | 'date' | 'search';

	let {
		type = 'text',
		value = $bindable(''),
		placeholder = '',
		disabled = false,
		required = false,
		error = '',
		label = '',
		name = '',
		id = '',
		min,
		max,
		step,
		icon,
		fullWidth = true
	}: {
		type?: InputType;
		value?: string | number;
		placeholder?: string;
		disabled?: boolean;
		required?: boolean;
		error?: string;
		label?: string;
		name?: string;
		id?: string;
		min?: number;
		max?: number;
		step?: number;
		icon?: Component;
		fullWidth?: boolean;
	} = $props();

	const inputId = $derived(id || name || `input-${Math.random().toString(36).substr(2, 9)}`);
	const hasError = $derived(error.length > 0);
	const widthClass = $derived(fullWidth ? 'w-full' : '');
</script>

<div class="space-y-2 {widthClass}">
	{#if label}
		<label for={inputId} class="block text-sm font-bold text-gray-300">
			{label}
			{#if required}
				<span class="text-red-500">*</span>
			{/if}
		</label>
	{/if}

	<div class="relative">
		{#if icon}
			{@const IconComponent = icon}
			<div class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">
				<IconComponent size={18} />
			</div>
		{/if}

		<input
			{type}
			{name}
			id={inputId}
			bind:value
			{placeholder}
			{disabled}
			{required}
			{min}
			{max}
			{step}
			class="w-full bg-background border rounded-lg py-2.5 text-white placeholder-gray-500 focus:outline-none focus:ring-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed
				{icon ? 'pl-11 pr-4' : 'px-4'}
				{hasError
				? 'border-red-500 focus:ring-red-500 focus:border-red-500'
				: 'border-border focus:ring-accent focus:border-transparent'}"
		/>
	</div>

	{#if hasError}
		<p class="text-sm text-red-500 mt-1">{error}</p>
	{/if}
</div>
