<script lang="ts">
	import { enhance } from '$app/forms';
	import { Lock, User } from 'lucide-svelte';
	import type { ActionData } from './$types';

	let { form }: { form?: ActionData } = $props();

	let isLoading = $state(false);
</script>

<svelte:head>
	<title>Login - Nexus Trade</title>
</svelte:head>

<div class="min-h-screen flex items-center justify-center bg-background p-4">
	<div class="w-full max-w-md">
		<!-- Logo/Title -->
		<div class="text-center mb-8">
			<h1 class="text-4xl font-black text-white mb-2">
				<span class="text-accent">Nexus</span> Trade
			</h1>
			<p class="text-gray-400">Crypto Futures Portfolio Manager</p>
		</div>

		<!-- Login Card -->
		<div class="bg-card border border-border rounded-2xl p-8 shadow-2xl">
			<h2 class="text-2xl font-bold mb-6">Sign In</h2>

			{#if form?.error}
				<div class="bg-red-500/10 border border-red-500 rounded-lg p-4 mb-6 text-red-500 text-sm">
					{form.error}
				</div>
			{/if}

			<form
				method="POST"
				use:enhance={() => {
					isLoading = true;
					return async ({ update }) => {
						await update();
						isLoading = false;
					};
				}}
			>
				<!-- Username Field -->
				<div class="mb-4">
					<label for="username" class="block text-sm font-bold mb-2 text-gray-300">
						Username
					</label>
					<div class="relative">
						<div class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">
							<User size={20} />
						</div>
						<input
							type="text"
							id="username"
							name="username"
							value={form?.username ?? ''}
							required
							class="w-full bg-background border border-border rounded-lg py-3 pl-11 pr-4 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
							placeholder="Enter your username"
						/>
					</div>
				</div>

				<!-- Password Field -->
				<div class="mb-6">
					<label for="password" class="block text-sm font-bold mb-2 text-gray-300">
						Password
					</label>
					<div class="relative">
						<div class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">
							<Lock size={20} />
						</div>
						<input
							type="password"
							id="password"
							name="password"
							required
							class="w-full bg-background border border-border rounded-lg py-3 pl-11 pr-4 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-accent focus:border-transparent transition-all"
							placeholder="Enter your password"
						/>
					</div>
				</div>

				<!-- Submit Button -->
				<button
					type="submit"
					disabled={isLoading}
					class="w-full bg-accent hover:bg-accent-hover text-background font-bold py-3 px-4 rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
				>
					{#if isLoading}
						<div class="animate-spin rounded-full h-5 w-5 border-b-2 border-background"></div>
						<span>Signing in...</span>
					{:else}
						Sign In
					{/if}
				</button>
			</form>

			<!-- Demo Credentials -->
			<div class="mt-6 p-4 bg-background border border-border rounded-lg">
				<p class="text-xs text-gray-400 font-bold uppercase mb-2">Demo Credentials</p>
				<p class="text-sm text-gray-300">
					Username: <span class="text-accent font-mono">demo</span>
				</p>
				<p class="text-sm text-gray-300">
					Password: <span class="text-accent font-mono">demo</span>
				</p>
			</div>
		</div>

		<!-- Footer -->
		<p class="text-center text-gray-500 text-sm mt-6">
			&copy; 2025 Nexus Trade. Advanced Trading Platform.
		</p>
	</div>
</div>

<style>
	/* Additional animations for loading spinner */
	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}
</style>
