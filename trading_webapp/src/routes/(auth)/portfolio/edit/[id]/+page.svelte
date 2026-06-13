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
		// Navigate back after successful update
		goto('/portfolio');
	}
</script>

<svelte:head>
	<title>Edit Portfolio - {data.portfolio?.name || 'Nexus Trade'}</title>
</svelte:head>

<div class="h-[calc(100vh-4rem)] bg-background">
	<DraftWizard
		portfolio={data.portfolio}
		portfolioId={data.portfolio?.id}
		onClose={handleClose}
		onSuccess={handleSuccess}
	/>
</div>
