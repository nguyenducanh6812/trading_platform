<script lang="ts">
	import { page } from '$app/stores';
	import { browser } from '$app/environment';
	import {
		LayoutDashboard,
		Briefcase,
		TrendingUp,
		History,
		Settings,
		X,
		Menu,
		ChevronLeft,
		ChevronRight
	} from 'lucide-svelte';

	let { isOpen = $bindable(true) }: { isOpen?: boolean } = $props();

	interface NavItem {
		label: string;
		href: string;
		icon: any;
	}

	const navItems: NavItem[] = [
		{ label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
		{ label: 'Portfolio', href: '/portfolio', icon: Briefcase },
		{ label: 'Markets', href: '/markets', icon: TrendingUp },
		{ label: 'Backtest', href: '/backtest', icon: History }
	];

	const isActive = (href: string) => $page.url.pathname === href;

	function toggleSidebar() {
		isOpen = !isOpen;
		// Persist to localStorage
		if (browser) {
			localStorage.setItem('sidebarOpen', isOpen ? 'true' : 'false');
		}
	}

	// Load sidebar state from localStorage on mount
	$effect(() => {
		if (browser) {
			const saved = localStorage.getItem('sidebarOpen');
			if (saved !== null) {
				isOpen = saved === 'true';
			}
		}
	});
</script>

<!-- Mobile overlay -->
{#if isOpen}
	<button
		class="fixed inset-0 bg-black/50 z-40 lg:hidden"
		onclick={toggleSidebar}
		aria-label="Close sidebar"
	></button>
{/if}

<!-- Sidebar -->
<aside
	class="fixed top-0 left-0 h-screen bg-card border-r border-border flex flex-col transition-all duration-300 z-50
		{isOpen ? 'w-64' : 'w-0 lg:w-20'}"
>
	<!-- Logo / Header -->
	<div class="h-16 flex items-center justify-between px-4 border-b border-border">
		{#if isOpen}
			<a href="/dashboard" class="font-black text-lg">
				<span class="text-accent">Nexus</span>
				<span class="text-white">Trade</span>
			</a>
			<div class="flex items-center gap-2">
				<!-- Desktop collapse button -->
				<button
					class="hidden lg:block text-gray-400 hover:text-white transition-colors p-1 hover:bg-white/5 rounded"
					onclick={toggleSidebar}
					aria-label="Collapse sidebar"
					title="Collapse sidebar"
				>
					<ChevronLeft size={20} />
				</button>
				<!-- Mobile close button -->
				<button
					class="lg:hidden text-gray-400 hover:text-white transition-colors"
					onclick={toggleSidebar}
					aria-label="Close sidebar"
				>
					<X size={20} />
				</button>
			</div>
		{:else}
			<button
				class="hidden lg:block w-full text-accent hover:text-accent-hover transition-colors"
				onclick={toggleSidebar}
				aria-label="Expand sidebar"
				title="Expand sidebar"
			>
				<ChevronRight size={24} class="mx-auto" />
			</button>
		{/if}
	</div>

	<!-- Navigation -->
	<nav class="flex-1 overflow-y-auto custom-scrollbar py-6">
		<ul class="space-y-2 px-3">
			{#each navItems as { label, href, icon: Icon }}
				<li>
					<a
						{href}
						class="flex items-center gap-3 px-3 py-2.5 rounded-lg font-medium transition-all
							{isActive(href) ? 'bg-accent/10 text-accent' : 'text-gray-400 hover:text-white hover:bg-white/5'}
							{!isOpen ? 'justify-center' : ''}"
						title={!isOpen ? label : undefined}
					>
						<Icon size={20} class="shrink-0" />
						{#if isOpen}
							<span class="truncate">{label}</span>
						{/if}
					</a>
				</li>
			{/each}
		</ul>
	</nav>

	<!-- Settings (bottom) -->
	<div class="border-t border-border p-3">
		<a
			href="/settings"
			class="flex items-center gap-3 px-3 py-2.5 rounded-lg font-medium text-gray-400 hover:text-white hover:bg-white/5 transition-all
				{!isOpen ? 'justify-center' : ''}"
			title={!isOpen ? 'Settings' : undefined}
		>
			<Settings size={20} class="shrink-0" />
			{#if isOpen}
				<span class="truncate">Settings</span>
			{/if}
		</a>
	</div>
</aside>

<!-- Spacer to prevent content from going under sidebar -->
<div class="hidden lg:block {isOpen ? 'w-64' : 'w-20'} shrink-0 transition-all duration-300"></div>
