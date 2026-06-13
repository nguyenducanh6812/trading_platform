<script lang="ts">
	import { Sidebar, Header } from '$lib/components/layout';
	import type { LayoutData } from './$types';

	let { data, children }: { data: LayoutData; children: any } = $props();

	let sidebarOpen = $state(true);

	function toggleSidebar() {
		sidebarOpen = !sidebarOpen;
	}
</script>

<div class="min-h-screen bg-background flex">
	<!-- Sidebar -->
	<Sidebar bind:isOpen={sidebarOpen} />

	<!-- Main content area -->
	<div class="flex-1 flex flex-col min-w-0">
		<!-- Header -->
		<Header user={data.user} onMenuClick={toggleSidebar} />

		<!-- Page content -->
		<main class="flex-1 overflow-y-auto custom-scrollbar">
			{@render children()}
		</main>
	</div>
</div>
