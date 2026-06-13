# Nexus Trade - Trading WebApp

A modern crypto futures portfolio management platform built with **SvelteKit**.

## Overview

Frontend web application for the Nexus Trade trading platform, integrating with:
- **Backend**: `trading_platform` (Spring Boot 3.4.4)
- **Backtest**: `ct_mpt` (Python MPT module)

## Tech Stack

- **SvelteKit** ^2.14.0 (Svelte 5 with runes)
- **TypeScript** ^5.7.0
- **Tailwind CSS** ^3.4.0
- **lucide-svelte** (icons)
- **jose** (JWT auth)
- **ofetch** (HTTP client)

## Implemented Features ✅

### Phase 1 & 2: Core Infrastructure
- ✅ SvelteKit project with TypeScript
- ✅ Tailwind CSS v3 with custom theme
- ✅ JWT authentication system
- ✅ Login page with form validation
- ✅ Dashboard page with stats cards
- ✅ Protected routes
- ✅ Logout functionality

## Getting Started

### Development

```bash
npm run dev
```

Visit http://localhost:5173

**Demo Credentials:**
- Username: `demo`
- Password: `demo`

### Build

```bash
npm run build
npm run preview
```

## Environment Variables

Copy `.env.example` to `.env` and configure:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
JWT_SECRET=your-secret-key-min-32-characters
```

## Project Structure

```
src/
├── routes/              # File-based routing
│   ├── login/           # Login page
│   ├── dashboard/       # Dashboard
│   └── +layout.svelte   # Root layout
├── lib/
│   ├── server/auth.ts   # JWT utilities
│   ├── components/      # UI components (TBD)
│   ├── api/             # API client (TBD)
│   └── types/           # TypeScript types (TBD)
├── hooks.server.ts      # Auth middleware
└── app.css              # Global styles
```

## Design System

- Background: `#0a0b0d`
- Card: `#16181c`
- Border: `#2d2f36`
- Accent: `#00ffbd` (cyan/teal)
- Font: Inter

## Next Steps

### Remaining Tasks
- [ ] API client layer
- [ ] TypeScript types from backend DTOs
- [ ] Base UI components
- [ ] Layout components (Sidebar, Header)
- [ ] Portfolio management
- [ ] Markets page
- [ ] Backtest integration

See full plan: `C:\Users\nguye\.claude\plans\swirling-swinging-axolotl.md`

## Backend Integration

Add CORS to Spring Boot backend:

```java
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:5173")
    .allowedMethods("GET", "POST", "PUT", "DELETE")
    .allowCredentials(true);
```

## Status

**Current**: Phase 1 & 2 Complete
**Next**: API Client & TypeScript Types
**Timeline**: 11-week implementation plan
