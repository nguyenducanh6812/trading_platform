import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		fs: {
			// Allow serving files from the project root
			strict: false
		},
		// Enable HTTP/2 for parallel resource loading
		warmup: {
			// Pre-compile commonly used routes on server start
			clientFiles: [
				'./src/routes/(auth)/dashboard/+page.svelte',
				'./src/routes/(auth)/portfolio/+page.svelte',
				'./src/lib/components/ui/**/*.svelte'
			]
		}
	},
	optimizeDeps: {
		// Force pre-bundle these dependencies
		include: [
			'lucide-svelte',
			'ofetch',
			'd3',
			'layercake',
			'jose',
			'nanoid',
			'zod',
			'svelte',
			'svelte/internal'
		],
		// Exclude packages that don't need pre-bundling
		exclude: [],
		esbuildOptions: {
			target: 'esnext',
			// Use more CPU cores for faster bundling
			treeShaking: true
		},
		// Force dependency cache even on config changes
		force: false,
		// Enable dependency caching across restarts
		holdUntilCrawlEnd: false
	},
	ssr: {
		// Fix CJS/ESM interop issues with esm-env
		noExternal: ['esm-env'],
		// Optimize SSR module resolution
		optimizeDeps: {
			include: ['esm-env']
		}
	},
	resolve: {
		// Ensure proper module resolution on Windows
		conditions: ['node', 'import', 'module', 'browser', 'default'],
		// Deduplicate packages to reduce bundle size
		dedupe: ['svelte']
	},
	build: {
		target: 'esnext',
		minify: 'esbuild',
		// Improve module resolution
		rollupOptions: {
			external: []
		},
		// Larger chunk size warning limit (default is 500kb)
		chunkSizeWarningLimit: 1000
	},
	// Enable better caching
	cacheDir: 'node_modules/.vite'
});
