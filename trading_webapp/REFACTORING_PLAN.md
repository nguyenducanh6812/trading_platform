# Code Review & Refactoring Plan

## Issues Identified

### 1. ❌ DRY Violation - Massive Code Duplication

**Problem:** The two-way sync pattern is duplicated in all 3 wizard steps.

**Current Code (repeated 3 times):**
```typescript
// WizardStep1, WizardStep2, WizardStep3 all have this pattern
let localValue = $state(data.field || defaultValue);
let lastParentValue = $state(data.field || defaultValue);

$effect(() => {
  if (data.field !== lastParentValue) {
    localValue = data.field || defaultValue;
    lastParentValue = data.field || defaultValue;
  }
});

$effect.pre(() => {
  data.field = localValue;
  lastParentValue = localValue;
});
```

**Impact:**
- ~50 lines of duplicate sync logic across 3 files
- Hard to maintain (changes need 3x updates)
- Error-prone

---

### 2. ❌ Single Responsibility Principle Violation

**Problem:** Wizard components handle BOTH UI rendering AND complex state synchronization.

**Should be separated into:**
- Presentation components (UI only)
- State management logic (sync utilities)

---

### 3. ❌ Magic Numbers/Strings

**Problem:** Hard-coded values scattered throughout:
```typescript
let initialCapital = $state(data.initialCapital || 10000); // Magic number
let selectedStrategyType = $state<StrategyType>(data.strategyType || 'MPT'); // Magic string
```

**Should be:**
```typescript
const DEFAULT_INITIAL_CAPITAL = 10000;
const DEFAULT_STRATEGY_TYPE = 'MPT';
```

---

### 4. ❌ Draft Data Duplication

**Problem:** Draft data object creation is duplicated 3 times in `DraftWizard.svelte`:
- Auto-save effect
- beforeunload handler
- handleClose function

---

### 5. ❌ Lack of TypeScript Strict Mode

**Problem:** No strict type checking enabled, potential runtime errors.

---

## Refactoring Solutions

### Solution 1: Create Reusable Sync Utilities

**File:** `src/lib/utils/twoWaySync.ts`

```typescript
import { browser } from '$app/environment';

/**
 * Creates a two-way synchronized state between local component and parent data
 * Handles draft loading and user input synchronization
 */
export function createSyncedFields<T extends Record<string, any>>(
  parentData: T,
  fields: Array<{
    key: keyof T;
    defaultValue: any;
  }>
) {
  const state = $state<Record<string, any>>({});
  const tracking = $state<Record<string, any>>({});

  // Initialize
  fields.forEach(({ key, defaultValue }) => {
    state[key] = parentData[key] ?? defaultValue;
    tracking[key] = parentData[key] ?? defaultValue;
  });

  // Sync FROM parent (draft loading)
  $effect(() => {
    fields.forEach(({ key, defaultValue }) => {
      const parentValue = parentData[key] ?? defaultValue;
      if (parentValue !== tracking[key]) {
        state[key] = parentValue;
        tracking[key] = parentValue;
      }
    });
  });

  // Sync TO parent (user input)
  $effect.pre(() => {
    fields.forEach(({ key }) => {
      parentData[key] = state[key];
      tracking[key] = state[key];
    });
  });

  return state;
}
```

---

### Solution 2: Extract Constants

**File:** `src/lib/constants/portfolio.ts`

```typescript
export const PORTFOLIO_DEFAULTS = {
  INITIAL_CAPITAL: 10000,
  STRATEGY_TYPE: 'MPT' as const,
  RISK_TOLERANCE: 'MODERATE' as const,
  REBALANCING_FREQUENCY: 'MONTHLY' as const,
  AUTO_REBALANCE: true,
  LEVERAGE_RATIO: 1,
  LEVERAGE_ENABLED: false,
  CURRENCY: 'USD' as const,
} as const;

export const VALIDATION = {
  MIN_CAPITAL: 100,
  MIN_NAME_LENGTH: 3,
  MAX_NAME_LENGTH: 100,
  MIN_LEVERAGE: 1,
  MAX_LEVERAGE: 10,
  MIN_WEIGHT: 99,
  MAX_WEIGHT: 101,
} as const;
```

---

### Solution 3: Refactor WizardStep1 (Example)

**Before (41 lines of sync logic):**
```typescript
let name = $state(data.name || '');
let initialCapital = $state(data.initialCapital || 10000);
// ... 4 fields
let lastParentName = $state(data.name || '');
// ... 4 tracking variables

$effect(() => {
  if (data.name !== lastParentName) {
    name = data.name || '';
    lastParentName = data.name || '';
  }
  // ... repeat for 4 fields
});

$effect.pre(() => {
  data.name = name;
  // ... repeat for 4 fields
  lastParentName = name;
  // ... repeat for 4 fields
});
```

**After (5 lines):**
```typescript
import { createSyncedFields } from '$lib/utils/twoWaySync';
import { PORTFOLIO_DEFAULTS } from '$lib/constants/portfolio';

const fields = createSyncedFields(data, [
  { key: 'name', defaultValue: '' },
  { key: 'initialCapital', defaultValue: PORTFOLIO_DEFAULTS.INITIAL_CAPITAL },
  { key: 'leverageEnabled', defaultValue: PORTFOLIO_DEFAULTS.LEVERAGE_ENABLED },
  { key: 'leverageRatio', defaultValue: PORTFOLIO_DEFAULTS.LEVERAGE_RATIO },
]);

// Access with fields.name, fields.initialCapital, etc.
```

---

### Solution 4: Extract Draft Data Helper

**File:** `src/lib/utils/draftHelpers.ts`

```typescript
import type { CreatePortfolioRequest } from '$lib/types';

/**
 * Creates a serializable draft data object from formData
 * Used for localStorage and beforeunload handlers
 */
export function createDraftData(
  formData: Partial<CreatePortfolioRequest>
): Partial<CreatePortfolioRequest> {
  return {
    name: formData.name,
    description: formData.description,
    initialCapital: formData.initialCapital,
    currency: formData.currency,
    strategyType: formData.strategyType,
    riskTolerance: formData.riskTolerance,
    rebalancingFrequency: formData.rebalancingFrequency,
    autoRebalance: formData.autoRebalance,
    leverageRatio: formData.leverageRatio,
    leverageEnabled: formData.leverageEnabled,
    positions: formData.positions,
  };
}
```

**Usage in DraftWizard.svelte:**
```typescript
// Before: 10+ lines duplicated 3 times
const draftData = {
  name: formData.name,
  description: formData.description,
  // ... 10 more fields
};

// After: 1 line
const draftData = createDraftData(formData);
```

---

### Solution 5: Add JSDoc Documentation

**Example:**
```typescript
/**
 * Two-way synchronized wizard step for portfolio core setup
 *
 * @component
 * @param {Object} props - Component props
 * @param {Partial<CreatePortfolioRequest>} props.data - Parent form data (two-way synced)
 * @param {Function} props.onComplete - Callback when step is completed
 *
 * @example
 * <WizardStep1 data={formData} onComplete={handleStep1Complete} />
 */
```

---

### Solution 6: Improve Error Handling

**Current:** Silent failures in some places

**Proposed:**
```typescript
// src/lib/utils/errorHandler.ts
export function handleApiError(error: unknown, context: string) {
  console.error(`[${context}] Error:`, error);

  if (error instanceof ApiClientError) {
    toast.error(error.message, {
      endpoint: error.details?.endpoint,
      statusCode: error.details?.statusCode,
    });
  } else {
    toast.error('An unexpected error occurred', {
      message: error instanceof Error ? error.message : String(error),
    });
  }
}
```

---

### Solution 7: Extract Validation Logic

**Current:** Validation scattered in components

**Proposed:**
```typescript
// src/lib/validation/portfolio.ts
import { VALIDATION } from '$lib/constants/portfolio';

export const portfolioValidation = {
  name: (value: string) => ({
    isValid: value.trim().length > 0,
    error: value.trim().length === 0 ? 'Portfolio name is required' : '',
  }),

  capital: (value: number) => ({
    isValid: value >= VALIDATION.MIN_CAPITAL,
    error: value < VALIDATION.MIN_CAPITAL
      ? `Minimum capital is $${VALIDATION.MIN_CAPITAL}`
      : '',
  }),

  leverageRatio: (value: number) => ({
    isValid: value >= VALIDATION.MIN_LEVERAGE && value <= VALIDATION.MAX_LEVERAGE,
    error: `Leverage must be between ${VALIDATION.MIN_LEVERAGE}x and ${VALIDATION.MAX_LEVERAGE}x`,
  }),
};
```

---

## Implementation Priority

### High Priority (Do First)
1. ✅ Extract constants → `src/lib/constants/portfolio.ts`
2. ✅ Create draft helper → `src/lib/utils/draftHelpers.ts`
3. ✅ Refactor DraftWizard to use draft helper (remove duplication)

### Medium Priority
4. ✅ Create two-way sync utility → `src/lib/utils/twoWaySync.ts`
5. ✅ Refactor WizardStep1 to use sync utility
6. ✅ Refactor WizardStep2 to use sync utility

### Low Priority (Nice to Have)
7. ⚠️ Extract validation logic
8. ⚠️ Add JSDoc comments
9. ⚠️ Improve error handling
10. ⚠️ Enable TypeScript strict mode

---

## Estimated Impact

### Before Refactoring
- **Lines of duplicate code:** ~150 lines
- **Maintainability:** Low (changes require 3x updates)
- **Readability:** Medium (lots of boilerplate)

### After Refactoring
- **Lines of duplicate code:** ~0 lines
- **Maintainability:** High (single source of truth)
- **Readability:** High (declarative, clear intent)
- **Code reduction:** ~40% less code in wizard steps

---

## Next Steps

1. Create utility files
2. Test utilities in isolation
3. Refactor one wizard step at a time
4. Verify functionality after each refactor
5. Remove old code once confirmed working
