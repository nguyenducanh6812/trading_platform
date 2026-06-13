<script lang="ts">
	import { toastStore } from '$lib/stores/toast';
	import Toast from './Toast.svelte';

	// Subscribe to global toast store
	const { toasts } = $derived(toastStore);
</script>

<!--
	Global Toast Container
	Positioned at top-right, stacks multiple toasts vertically
	Mounted once in root layout
-->
<div class="fixed top-4 right-4 z-50 flex flex-col gap-3 max-w-md pointer-events-none">
	{#each $toastStore.toasts as toast (toast.id)}
		<div class="pointer-events-auto">
			<Toast
				message={toast.message}
				type={toast.type}
				details={toast.details}
				onClose={() => toastStore.dismiss(toast.id)}
			/>
		</div>
	{/each}
</div>
