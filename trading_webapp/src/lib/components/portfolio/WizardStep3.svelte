<script lang="ts">
	import { onMount } from 'svelte';
	import { Button } from '$lib/components/ui';
	import { ChevronDown, ChevronRight, Loader2, Search, X, Plus } from 'lucide-svelte';
	import type { CreatePortfolioRequest, PositionInfo } from '$lib/types';
	import type { MarketResponse, InstrumentInfo, GetInstrumentsByMarketRequest } from '$lib/api';
	import { getAllMarkets, getInstrumentsByMarket } from '$lib/api';

	let {
		data,
		onComplete
	}: {
		data: Partial<CreatePortfolioRequest>;
		onComplete: (data: any) => void;
	} = $props();

	// State
	let markets = $state<MarketResponse[]>([]);
	let expandedMarkets = $state<Set<number>>(new Set()); // Empty = all collapsed by default
	let marketInstruments = $state<Record<number, InstrumentInfo[]>>({});
	let loadingMarkets = $state<Set<number>>(new Set());
	let marketErrors = $state<Record<number, string>>({}); // Track errors per market
	let isLoadingMarkets = $state(true);
	let marketsError = $state<string | null>(null);

	// Combobox state per market
	let marketSearchQueries = $state<Record<number, string>>({});
	let marketDropdownOpen = $state<Record<number, boolean>>({});

	// Selected instruments with their market codes
	interface SelectedInstrument {
		symbol: string;
		name: string;
		marketCode: string;
	}

	// Helper function to load instruments from positions
	function loadInstrumentsFromPositions(positions: PositionInfo[] | undefined) {
		if (!positions || positions.length === 0) {
			return { instruments: new Map(), weights: {} };
		}

		const instrumentMap = new Map<string, SelectedInstrument>();
		const weightMap: Record<string, number> = {};

		positions.forEach((p) => {
			const marketType = p.marketType || 'LINEAR';
			const key = `${p.instrumentCode}-${marketType}`;
			instrumentMap.set(key, {
				symbol: p.instrumentCode,
				name: p.instrumentName,
				marketCode: marketType
			});
			weightMap[key] = (p.weight || 0) * 100;
		});

		return { instruments: instrumentMap, weights: weightMap };
	}

	// Initialize from parent data
	const initial = loadInstrumentsFromPositions(data.positions);
	let selectedInstruments = $state<Map<string, SelectedInstrument>>(initial.instruments);
	let weights = $state<Record<string, number>>(initial.weights);

	// Track to prevent circular updates
	let isSyncingFromParent = $state(false);
	let lastSyncedSize = $state(selectedInstruments.size);

	// Load markets on mount
	onMount(async () => {
		try {
			console.log('[WizardStep3] Fetching markets...');
			markets = await getAllMarkets();
			console.log('[WizardStep3] Loaded markets:', markets);
			marketsError = null; // Clear any previous error
		} catch (error: any) {
			console.error('[WizardStep3] Failed to load markets:', error);
			marketsError = error.message || 'Failed to load markets. Please try again.';
		} finally {
			isLoadingMarkets = false;
		}
	});

	// Sync FROM parent when parent positions change externally (e.g., draft loading)
	$effect(() => {
		// Only sync from parent if positions exist and we're not already syncing
		if (!isSyncingFromParent && data.positions && data.positions.length > 0) {
			// Check if parent positions are different from current state
			if (data.positions.length !== selectedInstruments.size) {
				isSyncingFromParent = true;
				const loaded = loadInstrumentsFromPositions(data.positions);
				selectedInstruments = loaded.instruments;
				weights = loaded.weights;
				lastSyncedSize = selectedInstruments.size;
				isSyncingFromParent = false;
			}
		}
	});

	// Calculate total weight
	const totalWeight = $derived(
		Array.from(selectedInstruments.keys()).reduce((sum, key) => sum + (weights[key] || 0), 0)
	);

	// Sync TO parent when local changes (runs before DOM updates)
	$effect.pre(() => {
		// Only sync to parent if not syncing from parent and size changed
		if (!isSyncingFromParent && selectedInstruments.size !== lastSyncedSize) {
			const positions: PositionInfo[] = Array.from(selectedInstruments.entries()).map(
				([key, instrument]) => ({
					instrumentCode: instrument.symbol,
					instrumentName: instrument.name,
					marketType: instrument.marketCode,
					quantity: 0,
					weight: (weights[key] || 0) / 100,
					averageEntryPrice: 0,
					currentPrice: 0,
					unrealizedPnL: 0,
					realizedPnL: 0,
					status: 'PENDING' as const
				})
			);
			data.positions = positions;
			lastSyncedSize = selectedInstruments.size;
		}
	});

	// Validation
	const isValid = $derived(
		selectedInstruments.size > 0 && totalWeight >= 99 && totalWeight <= 101
	);

	// Filter instruments for a specific market's combobox
	function getFilteredInstruments(marketId: number): InstrumentInfo[] {
		const instruments = marketInstruments[marketId] || [];
		const query = marketSearchQueries[marketId]?.toLowerCase()?.trim() || '';

		console.log('[WizardStep3] Filtering:', {
			marketId,
			query,
			queryLength: query.length,
			instrumentsCount: instruments.length,
			sampleInstrument: instruments[0]
		});

		if (!query) return instruments;

		try {
			const filtered = instruments.filter((inst) => {
				const symbolMatch = inst.symbol?.toLowerCase().includes(query);
				const nameMatch = inst.name?.toLowerCase().includes(query);
				const baseMatch = inst.baseCurrency?.toLowerCase().includes(query);
				const quoteMatch = inst.quoteCurrency?.toLowerCase().includes(query);

				return symbolMatch || nameMatch || baseMatch || quoteMatch;
			});

			console.log('[WizardStep3] Filtered results:', {
				count: filtered.length,
				query,
				samples: filtered.slice(0, 3).map((i) => i.symbol)
			});

			return filtered;
		} catch (error) {
			console.error('[WizardStep3] Filter error:', error);
			return instruments;
		}
	}

	// Toggle market expansion and lazy load instruments
	async function toggleMarket(market: MarketResponse) {
		const marketId = market.id;
		if (expandedMarkets.has(marketId)) {
			// Collapse
			expandedMarkets.delete(marketId);
			expandedMarkets = new Set(expandedMarkets);
		} else {
			// Expand and lazy load instruments if not already loaded
			expandedMarkets.add(marketId);
			expandedMarkets = new Set(expandedMarkets);

			if (!marketInstruments[marketId] && !marketErrors[marketId]) {
				loadingMarkets.add(marketId);
				loadingMarkets = new Set(loadingMarkets);

				try {
					console.log(`[WizardStep3] Loading instruments for market: ${market.name} (ID: ${marketId})`);
					const request: GetInstrumentsByMarketRequest = {
						marketId: market.id,
						marketCode: market.code,
						marketName: market.name
					};
					const response = await getInstrumentsByMarket(request);
					// Trigger reactivity by creating new object
					marketInstruments = { ...marketInstruments, [marketId]: response.instruments };
					marketErrors = { ...marketErrors };
					delete marketErrors[marketId]; // Clear any previous error
					console.log(`[WizardStep3] Loaded ${response.instruments.length} instruments for ${market.name}`);
				} catch (error: any) {
					console.error(`[WizardStep3] Failed to load instruments for ${market.name}:`, error);
					marketErrors = { ...marketErrors, [marketId]: error.message || 'Failed to load instruments' };
				} finally {
					loadingMarkets.delete(marketId);
					loadingMarkets = new Set(loadingMarkets);
				}
			}
		}
	}

	// Retry loading instruments for a specific market
	async function retryLoadInstruments(market: MarketResponse) {
		const marketId = market.id;
		marketErrors = { ...marketErrors };
		delete marketErrors[marketId];
		loadingMarkets.add(marketId);
		loadingMarkets = new Set(loadingMarkets);

		try {
			console.log(`[WizardStep3] Retrying load for market: ${market.name} (ID: ${marketId})`);
			const request: GetInstrumentsByMarketRequest = {
				marketId: market.id,
				marketCode: market.code,
				marketName: market.name
			};
			const response = await getInstrumentsByMarket(request);
			marketInstruments = { ...marketInstruments, [marketId]: response.instruments };
			console.log(`[WizardStep3] Retry successful, loaded ${response.instruments.length} instruments`);
		} catch (error: any) {
			console.error(`[WizardStep3] Retry failed for ${market.name}:`, error);
			marketErrors = { ...marketErrors, [marketId]: error.message || 'Failed to load instruments' };
		} finally {
			loadingMarkets.delete(marketId);
			loadingMarkets = new Set(loadingMarkets);
		}
	}

	// Check if instrument is selected
	function isSelected(symbol: string, marketCode: string): boolean {
		return selectedInstruments.has(`${symbol}-${marketCode}`);
	}

	// Add instrument from combobox
	function selectInstrument(instrument: InstrumentInfo, marketCode: string) {
		const key = `${instrument.symbol}-${marketCode}`;

		// Don't add if already selected
		if (selectedInstruments.has(key)) return;

		selectedInstruments.set(key, {
			symbol: instrument.symbol,
			name: instrument.name,
			marketCode
		});

		// Auto-distribute weight equally
		const newWeight = 100 / selectedInstruments.size;
		Array.from(selectedInstruments.keys()).forEach((k) => {
			weights[k] = parseFloat(newWeight.toFixed(2));
		});

		// Trigger reactivity
		selectedInstruments = new Map(selectedInstruments);

		// Clear search and close dropdown
		marketSearchQueries[marketCode] = '';
		marketDropdownOpen[marketCode] = false;
	}

	// Remove instrument
	function removeInstrument(symbol: string, marketCode: string) {
		const key = `${symbol}-${marketCode}`;
		selectedInstruments.delete(key);
		delete weights[key];

		// Rebalance remaining instruments
		if (selectedInstruments.size > 0) {
			const newWeight = 100 / selectedInstruments.size;
			Array.from(selectedInstruments.keys()).forEach((k) => {
				weights[k] = parseFloat(newWeight.toFixed(2));
			});
		}

		// Trigger reactivity
		selectedInstruments = new Map(selectedInstruments);
	}

	// Toggle dropdown for a market's combobox
	function toggleDropdown(marketCode: string) {
		marketDropdownOpen[marketCode] = !marketDropdownOpen[marketCode];
	}

	// Close dropdown when clicking outside
	function handleClickOutside(event: MouseEvent, marketCode: string) {
		const target = event.target as HTMLElement;
		const comboboxContainer = target.closest('.combobox-container');

		if (!comboboxContainer) {
			marketDropdownOpen[marketCode] = false;
		}
	}

	// Auto-balance weights
	function autoBalance() {
		const equalWeight = 100 / selectedInstruments.size;
		Array.from(selectedInstruments.keys()).forEach((key) => {
			weights[key] = parseFloat(equalWeight.toFixed(2));
		});
	}

	function handleNext() {
		if (!isValid) return;

		const positions: PositionInfo[] = Array.from(selectedInstruments.entries()).map(
			([key, instrument]) => ({
				instrumentCode: instrument.symbol,
				instrumentName: instrument.name,
				marketType: instrument.marketCode,
				quantity: 0,
				weight: (weights[key] || 0) / 100,
				averageEntryPrice: 0,
				currentPrice: 0,
				unrealizedPnL: 0,
				realizedPnL: 0,
				status: 'PENDING' as const
			})
		);

		onComplete({ positions });
	}
</script>

<div class="max-w-3xl mx-auto">
	<h2 class="text-2xl font-black mb-2">Select Assets</h2>
	<p class="text-gray-400 mb-8">
		Choose crypto assets from any market type, then set their allocation weights
	</p>

	<div class="space-y-6">
		<!-- Markets Accordion -->
		<div>
			<div class="flex items-center justify-between mb-4">
				<p class="block text-sm font-bold text-white">
					Trading Instruments <span class="text-red-500">*</span>
				</p>
				<span class="text-sm text-gray-400">{selectedInstruments.size} selected</span>
			</div>

			{#if isLoadingMarkets}
				<!-- Loading State -->
				<div class="flex items-center justify-center py-12">
					<Loader2 class="animate-spin text-accent" size={32} />
					<span class="ml-3 text-gray-400">Loading markets...</span>
				</div>
			{:else if marketsError}
				<!-- Error State -->
				<div class="bg-red-900/20 border border-red-500 rounded-xl p-6 text-center">
					<div class="mb-4">
						<svg
							class="w-12 h-12 mx-auto text-red-500 mb-3"
							fill="none"
							stroke="currentColor"
							viewBox="0 0 24 24"
						>
							<path
								stroke-linecap="round"
								stroke-linejoin="round"
								stroke-width="2"
								d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
							></path>
						</svg>
						<h3 class="font-bold text-white mb-2">Failed to Load Markets</h3>
						<p class="text-sm text-red-400 mb-4">{marketsError}</p>
					</div>
					<button
						onclick={async () => {
							isLoadingMarkets = true;
							marketsError = null;
							try {
								markets = await getAllMarkets();
								console.log('[WizardStep3] Retry successful, loaded markets:', markets);
							} catch (error: any) {
								console.error('[WizardStep3] Retry failed:', error);
								marketsError = error.message || 'Failed to load markets. Please try again.';
							} finally {
								isLoadingMarkets = false;
							}
						}}
						class="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg font-medium transition-colors"
					>
						Retry
					</button>
				</div>
			{:else if markets.length === 0}
				<!-- Empty State -->
				<div class="text-center py-12">
					<p class="text-gray-500">No markets available</p>
				</div>
			{:else}
				<!-- Markets List with Comboboxes -->
				<div class="space-y-4">
					{#each markets as market}
						{@const isExpanded = expandedMarkets.has(market.id)}
						{@const isLoading = loadingMarkets.has(market.id)}
						{@const filteredInstruments = getFilteredInstruments(market.id)}
						{@const isDropdownOpen = marketDropdownOpen[market.id] || false}
						{@const searchQuery = marketSearchQueries[market.id] || ''}
						{@const shouldShowDropdown = isDropdownOpen && searchQuery.trim() && filteredInstruments.length > 0}
						{#if searchQuery}
							{(() => {
								console.log('[WizardStep3] Dropdown state:', {
									marketId: market.id,
									marketCode: market.code,
									isDropdownOpen,
									searchQuery,
									filteredCount: filteredInstruments.length,
									shouldShowDropdown
								});
								return '';
							})()}
						{/if}

						<div class="border border-border rounded-xl overflow-visible">
							<!-- Market Header -->
							<button
								onclick={() => toggleMarket(market)}
								class="w-full px-4 py-3 flex items-center justify-between bg-card hover:bg-background transition-colors rounded-t-xl"
							>
								<div class="flex items-center gap-3">
									{#if isExpanded}
										<ChevronDown size={20} class="text-accent" />
									{:else}
										<ChevronRight size={20} class="text-gray-500" />
									{/if}
									<div class="text-left">
										<h3 class="font-bold text-white">{market.name}</h3>
										<p class="text-xs text-gray-500">{market.description}</p>
									</div>
								</div>

								{#if isLoading}
									<Loader2 class="animate-spin text-accent" size={16} />
								{:else if !isExpanded}
									<span class="text-xs text-gray-500">Click to expand</span>
								{/if}
							</button>

							<!-- Combobox Section (Lazy Loaded) -->
							{#if isExpanded}
								<div class="p-4 bg-background border-t border-border rounded-b-xl">
									{#if isLoading}
										<div class="flex items-center justify-center py-8">
											<Loader2 class="animate-spin text-accent" size={24} />
											<span class="ml-3 text-gray-400 text-sm">Loading instruments...</span>
										</div>
									{:else if marketErrors[market.id]}
										<!-- Error State -->
										<div class="bg-red-900/20 border border-red-500/50 rounded-lg p-4 text-center">
											<svg
												class="w-8 h-8 mx-auto text-red-500 mb-2"
												fill="none"
												stroke="currentColor"
												viewBox="0 0 24 24"
											>
												<path
													stroke-linecap="round"
													stroke-linejoin="round"
													stroke-width="2"
													d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
												></path>
											</svg>
											<p class="text-sm text-red-400 mb-3">{marketErrors[market.id]}</p>
											<button
												onclick={() => retryLoadInstruments(market)}
												class="px-3 py-1.5 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg font-medium transition-colors"
											>
												Retry
											</button>
										</div>
									{:else}
										<!-- Combobox -->
										<div class="relative combobox-container">
											<p class="block text-sm font-medium text-gray-400 mb-2">
												Search and select instruments
											</p>

											<!-- Search Input -->
											<div class="relative">
												<Search
													class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500"
													size={18}
												/>
												<input
													type="text"
													bind:value={marketSearchQueries[market.id]}
													onfocus={() => {
														marketDropdownOpen = { ...marketDropdownOpen, [market.id]: true };
													}}
													oninput={(e) => {
														// Update state and open/close dropdown based on input
														const value = (e.target as HTMLInputElement).value;
														marketSearchQueries = { ...marketSearchQueries, [market.id]: value };

														if (value?.trim()) {
															// Open dropdown when typing
															marketDropdownOpen = { ...marketDropdownOpen, [market.id]: true };
														} else {
															// Close dropdown when input is cleared
															marketDropdownOpen = { ...marketDropdownOpen, [market.id]: false };
														}

														console.log('[WizardStep3] Input:', { marketCode: market.code, value, dropdownOpen: marketDropdownOpen[market.id] });
													}}
													onblur={(e) => {
														// Delay to allow click on dropdown items
														setTimeout(() => {
															const relatedTarget = e.relatedTarget as HTMLElement;
															if (!relatedTarget || !relatedTarget.closest('.combobox-container')) {
																marketDropdownOpen[market.id] = false;
															}
														}, 150);
													}}
													placeholder="Type to search instruments (e.g., BTC, ETH)..."
													class="w-full pl-10 pr-4 py-3 bg-card border border-border rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-accent transition-colors"
												/>
											</div>

											<!-- Dropdown -->
											{#if shouldShowDropdown}
												<div
													class="absolute z-50 w-full mt-1 bg-card border border-accent rounded-lg shadow-2xl max-h-64 overflow-y-auto"
												>
													{#each filteredInstruments as instrument}
														{@const alreadySelected = isSelected(instrument.symbol, market.code)}
														<button
															onclick={() => selectInstrument(instrument, market.code)}
															disabled={alreadySelected}
															class="w-full px-4 py-3 flex items-center justify-between hover:bg-background transition-colors text-left {alreadySelected
																? 'opacity-50 cursor-not-allowed'
																: ''}"
														>
															<div>
																<p class="font-bold text-white text-sm">{instrument.symbol}</p>
																<p class="text-xs text-gray-500">
																	{instrument.baseCurrency}/{instrument.quoteCurrency}
																</p>
															</div>
															{#if alreadySelected}
																<span class="text-xs text-accent">Selected</span>
															{:else}
																<Plus size={16} class="text-gray-500" />
															{/if}
														</button>
													{/each}
												</div>
											{/if}

											<!-- No results message -->
											{#if isDropdownOpen && searchQuery.trim() && filteredInstruments.length === 0}
												<div
													class="absolute z-50 w-full mt-1 bg-card border border-border rounded-lg shadow-2xl px-4 py-3"
												>
													<p class="text-sm text-gray-500">
														No instruments found matching "{searchQuery}"
													</p>
												</div>
											{:else if isDropdownOpen && !searchQuery.trim()}
												<div
													class="absolute z-50 w-full mt-1 bg-card border border-border rounded-lg shadow-2xl px-4 py-3"
												>
													<p class="text-sm text-gray-400">
														Type to search instruments (e.g., BTC, ETH, SOL)
													</p>
												</div>
											{/if}
										</div>

										<!-- Selected Instruments from This Market -->
										{@const selectedFromMarket = Array.from(selectedInstruments.entries()).filter(
											([key]) => key.endsWith(`-${market.code}`)
										)}
										{#if selectedFromMarket.length > 0}
											<div class="mt-4">
												<p class="text-xs font-medium text-gray-400 mb-2">
													Selected from this market:
												</p>
												<div class="flex flex-wrap gap-2">
													{#each selectedFromMarket as [key, instrument]}
														<button
															onclick={() => removeInstrument(instrument.symbol, market.code)}
															class="px-3 py-1.5 bg-accent/10 border border-accent rounded-lg flex items-center gap-2 hover:bg-accent/20 transition-colors group"
														>
															<span class="text-sm font-bold text-accent">{instrument.symbol}</span>
															<X
																size={14}
																class="text-accent group-hover:text-white transition-colors"
															/>
														</button>
													{/each}
												</div>
											</div>
										{/if}
									{/if}
								</div>
							{/if}
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<!-- Weight Allocation -->
		{#if selectedInstruments.size > 0}
			<div class="bg-card border border-border rounded-xl p-6">
				<div class="flex items-center justify-between mb-4">
					<h4 class="font-bold text-white">Allocation Weights</h4>
					<button
						onclick={autoBalance}
						class="text-sm text-accent hover:text-accent-hover font-medium transition-colors"
					>
						Auto-Balance
					</button>
				</div>

				<div class="space-y-3">
					{#each Array.from(selectedInstruments.entries()) as [key, instrument]}
						<div>
							<div class="flex items-center justify-between mb-2">
								<div class="flex items-center gap-2">
									<span class="text-sm font-medium text-white">{instrument.name}</span>
									<span class="text-xs text-gray-500">
										({markets.find((m) => m.code === instrument.marketCode)?.name || instrument.marketCode})
									</span>
								</div>
								<span class="text-sm font-bold text-accent">{weights[key] || 0}%</span>
							</div>
							<input
								type="range"
								bind:value={weights[key]}
								min={0}
								max={100}
								step={0.1}
								class="w-full h-2 rounded-lg appearance-none bg-border accent-accent"
							/>
						</div>
					{/each}
				</div>

				<!-- Total Weight Indicator -->
				<div class="mt-4 pt-4 border-t border-border">
					<div class="flex items-center justify-between">
						<span class="text-sm font-medium text-gray-400">Total Allocation</span>
						<span
							class="text-lg font-black {totalWeight >= 99 && totalWeight <= 101
								? 'text-green-500'
								: 'text-red-500'}"
						>
							{totalWeight.toFixed(2)}%
						</span>
					</div>
					{#if totalWeight < 99 || totalWeight > 101}
						<p class="text-xs text-red-500 mt-1">Total must equal 100%</p>
					{/if}
				</div>
			</div>

			<!-- Preview -->
			<div class="bg-accent/10 border border-accent rounded-xl p-6">
				<h4 class="font-bold text-white mb-3">Portfolio Composition</h4>
				<div class="space-y-2">
					{#each Array.from(selectedInstruments.entries()).sort( ([keyA], [keyB]) => (weights[keyB] || 0) - (weights[keyA] || 0) ) as [key, instrument]}
						<div class="flex items-center gap-3 group">
							<div
								class="w-8 h-8 rounded-full bg-accent text-background flex items-center justify-center text-xs font-black shrink-0"
							>
								{instrument.symbol.substring(0, 2)}
							</div>
							<div class="flex-1 min-w-0">
								<p class="text-sm font-medium text-white truncate">{instrument.name}</p>
								<p class="text-xs text-gray-500 truncate">
									{markets.find((m) => m.code === instrument.marketCode)?.name || instrument.marketCode}
								</p>
							</div>
							<div class="flex items-center gap-2">
								<div class="w-32 h-2 bg-background rounded-full overflow-hidden">
									<div
										class="h-full bg-accent transition-all"
										style="width: {weights[key] || 0}%"
									></div>
								</div>
								<span class="text-sm font-bold text-accent w-12 text-right"
									>{weights[key] || 0}%</span
								>
								<button
									onclick={() => removeInstrument(instrument.symbol, instrument.marketCode)}
									class="ml-2 p-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 border border-red-500/50 hover:border-red-500 transition-all opacity-0 group-hover:opacity-100"
									title="Remove {instrument.symbol}"
									aria-label="Remove {instrument.symbol}"
								>
									<X size={14} class="text-red-500" />
								</button>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<!-- Next Button -->
		<div class="flex justify-end pt-4">
			<Button onclick={handleNext} disabled={!isValid}>Next: Backtest & Deploy</Button>
		</div>
	</div>
</div>
