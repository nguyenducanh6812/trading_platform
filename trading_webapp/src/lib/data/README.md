# Market and Instrument Data Management

This directory contains all centralized static reference data for the trading application.

## 📁 File Structure

```
src/lib/data/
├── markets.ts          # Market types and instruments data
└── README.md          # This file
```

---

## 📊 markets.ts - Central Data File

### Overview

Contains all static reference data for portfolio creation:
- **Market Types**: SPOT, LINEAR, INVERSE, OPTION
- **Instruments**: Cryptocurrencies available per market type
- **Helper Functions**: Utility functions to query the data

---

## 🔧 How to Maintain

### Adding a New Instrument

**Example: Add XRP to LINEAR market**

```typescript
// In src/lib/data/markets.ts

export const INSTRUMENTS_BY_MARKET: Record<BybitMarketType, InstrumentData[]> = {
  // ... other markets
  LINEAR: [
    // ... existing instruments
    {
      code: 'XRP',              // Symbol code
      name: 'Ripple',           // Full name
      description: 'USDT-margined XRP perpetual',  // Description
      icon: 'XRP'               // Display icon/symbol
    }
  ]
};
```

**Steps:**
1. Open `src/lib/data/markets.ts`
2. Find the relevant market type (`SPOT`, `LINEAR`, `INVERSE`, or `OPTION`)
3. Add the new instrument object to the array
4. No recompilation needed - just save and reload!

---

### Removing an Instrument

**Example: Remove MATIC from LINEAR market**

Simply delete the object from the array:

```typescript
LINEAR: [
  { code: 'BTC', ... },
  { code: 'ETH', ... },
  // { code: 'MATIC', ... },  ← Comment out or delete
]
```

---

### Adding a New Market Type

**Example: Add FUTURES market**

1. **Update type definition** in `src/lib/types/portfolio.ts`:
```typescript
export type BybitMarketType = 'SPOT' | 'LINEAR' | 'INVERSE' | 'OPTION' | 'FUTURES';
```

2. **Add to MARKET_TYPES** in `src/lib/data/markets.ts`:
```typescript
export const MARKET_TYPES: Record<BybitMarketType, MarketTypeInfo> = {
  // ... existing markets
  FUTURES: {
    type: 'FUTURES',
    name: 'Futures Trading',
    description: 'Quarterly and bi-quarterly futures contracts',
    enabled: true
  }
};
```

3. **Add instruments** for the new market:
```typescript
export const INSTRUMENTS_BY_MARKET: Record<BybitMarketType, InstrumentData[]> = {
  // ... existing markets
  FUTURES: [
    { code: 'BTC', name: 'Bitcoin', description: 'BTC quarterly futures', icon: '₿' }
  ]
};
```

4. **Add icon** in WizardStep3.svelte:
```typescript
const marketTypeIcons: Record<BybitMarketType, any> = {
  // ... existing icons
  FUTURES: Calendar  // Import from lucide-svelte
};
```

---

### Disabling a Market Type

Set `enabled: false` to hide from UI:

```typescript
export const MARKET_TYPES: Record<BybitMarketType, MarketTypeInfo> = {
  OPTION: {
    type: 'OPTION',
    name: 'Options',
    description: 'Cryptocurrency options trading',
    enabled: false  // ← Disable OPTIONS market
  }
};
```

---

## 🎯 Usage Examples

### Import in Components

```typescript
import {
  MARKET_TYPES,
  INSTRUMENTS_BY_MARKET,
  getInstrumentsForMarket,
  findInstrument,
  isInstrumentAvailable
} from '$lib/data/markets';
```

### Get Instruments for a Market

```typescript
// Get all LINEAR instruments
const linearInstruments = getInstrumentsForMarket('LINEAR');

// Results: [{ code: 'BTC', ... }, { code: 'ETH', ... }, ...]
```

### Find Specific Instrument

```typescript
// Find BTC in LINEAR market
const btc = findInstrument('BTC', 'LINEAR');

// Result: { code: 'BTC', name: 'Bitcoin', ... }
```

### Check Availability

```typescript
// Check if SOL is available in SPOT market
const isAvailable = isInstrumentAvailable('SOL', 'SPOT');

// Result: false (SOL only in LINEAR)
```

### Get All Market Types

```typescript
// Get all enabled market types
const markets = Object.values(MARKET_TYPES).filter(m => m.enabled);

// Result: [{ type: 'SPOT', ... }, { type: 'LINEAR', ... }, ...]
```

---

## 📝 Data Structure Reference

### MarketTypeInfo

```typescript
interface MarketTypeInfo {
  type: BybitMarketType;    // 'SPOT' | 'LINEAR' | 'INVERSE' | 'OPTION'
  name: string;             // Display name
  description: string;      // Description text
  enabled: boolean;         // Show in UI?
}
```

### InstrumentData

```typescript
interface InstrumentData {
  code: string;            // Symbol code (e.g., 'BTC')
  name: string;            // Full name (e.g., 'Bitcoin')
  description: string;     // Market-specific description
  icon: string;            // Display icon/symbol
}
```

---

## ✅ Best Practices

### DO ✓

- ✓ Keep descriptions consistent within market types
- ✓ Use standard cryptocurrency symbols for icons (₿, Ξ)
- ✓ Sort instruments by market cap or importance
- ✓ Test in UI after adding new instruments
- ✓ Keep naming consistent (e.g., "USDT-margined X perpetual")

### DON'T ✗

- ✗ Don't add duplicate instruments in same market
- ✗ Don't use special characters in `code` field
- ✗ Don't forget to update all relevant markets
- ✗ Don't commit API keys or sensitive data
- ✗ Don't remove instruments that exist in user portfolios

---

## 🔄 Migration from Backend API

If you later decide to fetch from backend API:

```typescript
// src/lib/data/markets.ts - Add fallback logic

import { apiClient } from '$lib/api';

// Default static data
const DEFAULT_INSTRUMENTS = { ... };

// Try to fetch from backend, fallback to static
export async function getInstrumentsWithFallback(marketType: BybitMarketType) {
  try {
    const data = await apiClient(`/markets/${marketType}/instruments`);
    return data;
  } catch {
    // Backend offline - use static data
    return INSTRUMENTS_BY_MARKET[marketType];
  }
}
```

---

## 📚 Related Files

- `src/lib/types/portfolio.ts` - Type definitions
- `src/lib/components/portfolio/WizardStep3.svelte` - Uses this data
- `src/routes/(auth)/markets/+page.svelte` - Markets display page

---

## 🚀 Quick Reference

| Task | Location | File |
|------|----------|------|
| Add instrument | `INSTRUMENTS_BY_MARKET` | markets.ts |
| Add market type | `MARKET_TYPES` | markets.ts |
| Disable market | `enabled: false` | markets.ts |
| Change descriptions | `description` field | markets.ts |
| Update UI icons | `marketTypeIcons` | WizardStep3.svelte |

---

**Last Updated**: 2025-12-27
**Maintainer**: Development Team
