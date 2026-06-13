<script lang="ts">
	import { fade, scale } from 'svelte/transition';
	import { quintOut } from 'svelte/easing';
	import { X } from 'lucide-svelte';
	import type { Snippet } from 'svelte';

	type Size = 'sm' | 'md' | 'lg' | 'xl' | 'full';

	let {
		isOpen = $bindable(false),
		title,
		description,
		size = 'md',
		closeOnBackdrop = true,
		closeOnEscape = true,
		showCloseButton = true,
		children,
		footer
	}: {
		isOpen?: boolean;
		title?: string;
		description?: string;
		size?: Size;
		closeOnBackdrop?: boolean;
		closeOnEscape?: boolean;
		showCloseButton?: boolean;
		children: Snippet;
		footer?: Snippet;
	} = $props();

	const sizes = {
		sm: 'max-w-md',
		md: 'max-w-lg',
		lg: 'max-w-2xl',
		xl: 'max-w-4xl',
		full: 'max-w-7xl'
	};

	function close() {
		isOpen = false;
	}

	function handleBackdropClick(e: MouseEvent) {
		if (closeOnBackdrop && e.target === e.currentTarget) {
			close();
		}
	}

	function handleKeydown(e: KeyboardEvent) {
		if (closeOnEscape && e.key === 'Escape') {
			close();
		}
	}
</script>

<svelte:window onkeydown={handleKeydown} />

{#if isOpen}
	<!-- Backdrop -->
	<div
		class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
		transition:fade={{ duration: 200 }}
		onclick={handleBackdropClick}
		role="presentation"
	>
		<!-- Modal Content -->
		<div
			class="bg-card border border-border rounded-2xl shadow-2xl {sizes[size]} w-full max-h-[90vh] flex flex-col"
			transition:scale={{ duration: 200, easing: quintOut, start: 0.95 }}
			role="dialog"
			aria-modal="true"
			aria-labelledby={title ? 'modal-title' : undefined}
			aria-describedby={description ? 'modal-description' : undefined}
		>
			<!-- Header -->
			{#if title || showCloseButton}
				<div class="flex items-start justify-between p-6 border-b border-border">
					<div class="flex-1">
						{#if title}
							<h2 id="modal-title" class="text-xl font-bold text-white">
								{title}
							</h2>
						{/if}
						{#if description}
							<p id="modal-description" class="text-sm text-gray-400 mt-1">
								{description}
							</p>
						{/if}
					</div>

					{#if showCloseButton}
						<button
							type="button"
							onclick={close}
							class="ml-4 text-gray-400 hover:text-white transition-colors p-1 rounded-lg hover:bg-white/5"
							aria-label="Close modal"
						>
							<X size={20} />
						</button>
					{/if}
				</div>
			{/if}

			<!-- Body -->
			<div class="overflow-hidden flex-1">
				{@render children()}
			</div>

			<!-- Footer -->
			{#if footer}
				<div class="p-6 border-t border-border">
					{@render footer()}
				</div>
			{/if}
		</div>
	</div>
{/if}

<style>
	/* Prevent body scroll when modal is open */
	:global(body:has(.fixed)) {
		overflow: hidden;
	}
</style>
