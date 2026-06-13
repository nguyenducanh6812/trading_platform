<script lang="ts">
	import { Modal, Button } from '$lib/components/ui';

	let {
		isOpen,
		title,
		message,
		confirmText = 'Confirm',
		cancelText = 'Cancel',
		variant = 'danger',
		onConfirm,
		onCancel
	}: {
		isOpen: boolean;
		title: string;
		message: string;
		confirmText?: string;
		cancelText?: string;
		variant?: 'primary' | 'danger';
		onConfirm: () => void;
		onCancel: () => void;
	} = $props();
</script>

{#if isOpen}
	<Modal {isOpen} size="sm">
		<div class="p-6">
			<h3 class="text-xl font-bold mb-3">{title}</h3>
			<p class="text-gray-400 mb-6">{message}</p>

			<div class="flex gap-3 justify-end">
				<Button variant="secondary" onclick={onCancel}>
					{cancelText}
				</Button>
				<Button {variant} onclick={onConfirm}>
					{confirmText}
				</Button>
			</div>
		</div>
	</Modal>
{/if}
