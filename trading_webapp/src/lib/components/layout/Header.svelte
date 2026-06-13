<script lang="ts">
	import { enhance } from '$app/forms';
	import { Menu, Search, Bell, LogOut, User } from 'lucide-svelte';
	import type { User as UserType } from '$lib/types';

	let {
		user,
		onMenuClick
	}: {
		user: UserType;
		onMenuClick?: () => void;
	} = $props();

	let searchQuery = $state('');
	let showNotifications = $state(false);
</script>

<header class="sticky top-0 h-16 bg-card/95 backdrop-blur-sm border-b border-border z-30">
	<div class="h-full px-4 lg:px-6 flex items-center justify-between gap-4">
		<!-- Mobile menu button -->
		<button
			class="lg:hidden text-gray-400 hover:text-white transition-colors"
			onclick={onMenuClick}
			aria-label="Toggle menu"
		>
			<Menu size={24} />
		</button>

		<!-- Search bar (desktop) -->
		<div class="hidden md:flex flex-1 max-w-md">
			<div class="relative w-full">
				<div class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">
					<Search size={18} />
				</div>
				<input
					type="search"
					bind:value={searchQuery}
					placeholder="Search portfolios, strategies..."
					class="w-full bg-background border border-border rounded-lg py-2 pl-10 pr-4 text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
				/>
			</div>
		</div>

		<!-- Right side actions -->
		<div class="flex items-center gap-3">
			<!-- Notifications -->
			<button
				class="relative p-2 text-gray-400 hover:text-white hover:bg-white/5 rounded-lg transition-all"
				onclick={() => (showNotifications = !showNotifications)}
				aria-label="Notifications"
			>
				<Bell size={20} />
				<!-- Notification badge -->
				<span
					class="absolute top-1.5 right-1.5 w-2 h-2 bg-accent rounded-full border border-card"
				></span>
			</button>

			<!-- User menu -->
			<div class="flex items-center gap-3 pl-3 border-l border-border">
				<div class="hidden sm:block text-right">
					<p class="text-sm font-medium text-white">{user.username}</p>
					<p class="text-xs text-gray-400">{user.email}</p>
				</div>

				<div
					class="w-9 h-9 rounded-full bg-accent/10 border-2 border-accent flex items-center justify-center"
				>
					<User size={18} class="text-accent" />
				</div>

				<!-- Logout -->
				<form method="POST" action="/dashboard?/logout" use:enhance>
					<button
						type="submit"
						class="p-2 text-gray-400 hover:text-red-500 hover:bg-red-500/10 rounded-lg transition-all"
						aria-label="Logout"
						title="Logout"
					>
						<LogOut size={18} />
					</button>
				</form>
			</div>
		</div>
	</div>
</header>

<!-- Notifications dropdown (placeholder) -->
{#if showNotifications}
	<div class="absolute right-4 top-20 w-80 bg-card border border-border rounded-xl shadow-2xl p-4 z-50">
		<h3 class="font-bold text-white mb-3">Notifications</h3>
		<p class="text-sm text-gray-400">No new notifications</p>
	</div>
{/if}
