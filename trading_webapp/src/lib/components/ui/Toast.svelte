<script lang="ts">
	import { AlertCircle, CheckCircle, X, ChevronDown, ChevronUp } from 'lucide-svelte';

	let {
		message = '',
		details,
		type = 'error',
		onClose
	}: {
		message: string;
		details?: {
			endpoint?: string;
			method?: string;
			statusCode?: number;
			errorType?: string;
			timestamp?: string;
			stack?: string;
			message?: string;
			response?: any; // Backend response data
		};
		type?: 'success' | 'error' | 'warning';
		onClose: () => void;
	} = $props();

	let showDetails = $state(false);

	// Log error to console for debugging
	$effect(() => {
		if (type === 'error') {
			console.error('='.repeat(80));
			console.error('[Toast Error]', message);
			if (details) {
				console.error('Details:', details);
			}
			console.error('='.repeat(80));
		}
	});

	const styles = {
		success: 'bg-green-900 border-green-500',
		error: 'bg-red-900 border-red-500',
		warning: 'bg-yellow-900 border-yellow-500'
	};

	const textStyles = {
		success: 'text-green-500',
		error: 'text-red-500',
		warning: 'text-yellow-500'
	};

	const Icon = $derived(type === 'success' ? CheckCircle : AlertCircle);
</script>

<div
	class="w-full max-w-xl border rounded-xl shadow-2xl animate-in slide-in-from-right {styles[
		type
	]}"
>
	<!-- Header -->
	<div class="flex items-start gap-3 p-4">
		<Icon size={20} class={textStyles[type]} />
		<div class="flex-1 min-w-0">
			<p class="font-bold {textStyles[type]} text-sm mb-1">
				{type === 'error' ? 'Error' : type === 'warning' ? 'Warning' : 'Success'}
			</p>
			<p class="text-sm text-white">{message}</p>
		</div>
		<button onclick={onClose} class="text-gray-400 hover:text-white transition-colors">
			<X size={18} />
		</button>
	</div>

	<!-- Details Section -->
	{#if details}
		<div class="border-t border-white/10">
			<button
				onclick={() => (showDetails = !showDetails)}
				class="w-full px-4 py-2 flex items-center justify-between text-xs text-gray-400 hover:text-white transition-colors"
			>
				<span>Technical Details</span>
				{#if showDetails}
					<ChevronUp size={14} />
				{:else}
					<ChevronDown size={14} />
				{/if}
			</button>

			{#if showDetails}
				<div class="px-4 pb-4 space-y-2 text-xs font-mono">
					{#if details.endpoint}
						<div class="bg-background rounded p-2">
							<span class="text-gray-500">Endpoint:</span>
							<span class="text-white ml-2">{details.method || 'GET'} {details.endpoint}</span>
						</div>
					{/if}

					{#if details.errorType}
						<div class="bg-background rounded p-2">
							<span class="text-gray-500">Error Type:</span>
							<span class="{textStyles[type]} ml-2">{details.errorType}</span>
						</div>
					{/if}

					{#if details.statusCode}
						<div class="bg-background rounded p-2">
							<span class="text-gray-500">Status Code:</span>
							<span class="{textStyles[type]} ml-2">{details.statusCode}</span>
						</div>
					{/if}

					{#if details.timestamp}
						<div class="bg-background rounded p-2">
							<span class="text-gray-500">Time:</span>
							<span class="text-white ml-2">{new Date(details.timestamp).toLocaleString()}</span>
						</div>
					{/if}

					{#if details.stack}
						<div class="bg-background rounded p-2 max-h-32 overflow-y-auto">
							<p class="text-gray-500 mb-1">Stack Trace:</p>
							<pre class="text-gray-400 text-[10px] whitespace-pre-wrap">{details.stack}</pre>
						</div>
					{/if}

					{#if details.response}
						<div class="bg-background rounded p-2 max-h-40 overflow-y-auto">
							<p class="text-gray-500 mb-1">Backend Response:</p>
							<pre class="text-gray-400 text-[10px] whitespace-pre-wrap">{JSON.stringify(
									details.response,
									null,
									2
								)}</pre>
						</div>
					{/if}
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	@keyframes slide-in-from-right {
		from {
			transform: translateX(100%);
			opacity: 0;
		}
		to {
			transform: translateX(0);
			opacity: 1;
		}
	}

	.animate-in {
		animation: slide-in-from-right 0.3s ease-out;
	}
</style>
