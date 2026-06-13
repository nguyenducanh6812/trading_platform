<script lang="ts">
	import { goto } from '$app/navigation';
	import { DraftWizard } from '$lib/components/portfolio';
	import type { PageData } from './$types';

	let { data }: { data: PageData } = $props();

	function handleClose() {
		// Navigate back to portfolio list
		goto('/portfolio');
	}

	function handleSuccess() {
		// Navigate back after successful creation
		goto('/portfolio');
	}
</script>

<svelte:head>
	<title>Create Portfolio - Nexus Trade</title>
</svelte:head>

<div class="h-[calc(100vh-4rem)] bg-background">
	<DraftWizard portfolio={undefined} onClose={handleClose} onSuccess={handleSuccess} />
</div>
