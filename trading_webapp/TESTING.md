# Testing Instructions - Nexus Trade WebApp

This document provides comprehensive testing instructions for the current implementation (Phase 1 & 2).

## 🚀 Quick Start

### 1. Start the Development Server

```bash
cd D:\work\trading_webapp
npm run dev
```

The server will start on **http://localhost:5173**

---

## 🧪 Test Cases

### Test Case 1: Login Flow

**Steps:**
1. Visit http://localhost:5173
2. You should be automatically redirected to `/login`
3. Enter credentials:
   - Username: `demo`
   - Password: `demo`
4. Click "Sign In"

**Expected Result:**
- ✅ You should be redirected to `/dashboard`
- ✅ No error messages displayed

---

### Test Case 2: Protected Routes

**Steps:**
1. While logged in, visit http://localhost:5173/dashboard
2. Open browser DevTools → Application → Cookies (Chrome) or Storage → Cookies (Firefox)
3. Look for cookies under `localhost:5173`

**Expected Result:**
- ✅ You should see the dashboard (not redirected to login)
- ✅ Two cookies should be present:
  - `auth_token` (15-minute expiry)
  - `refresh_token` (7-day expiry)
- ✅ Both cookies should have `HttpOnly` flag

---

### Test Case 3: Logout Flow

**Steps:**
1. On the dashboard, click the red "Logout" button in the top-right
2. Check the URL

**Expected Result:**
- ✅ You should be redirected to `/login`
- ✅ Check cookies in DevTools - both `auth_token` and `refresh_token` should be cleared

---

### Test Case 4: Session Persistence

**Steps:**
1. Log in successfully (demo/demo)
2. Refresh the page (F5 or Ctrl+R)

**Expected Result:**
- ✅ You should remain logged in on the dashboard
- ✅ No redirect to login page

---

### Test Case 5: Invalid Credentials

**Steps:**
1. On the login page, enter:
   - Username: `demo`
   - Password: `wrong`
2. Click "Sign In"

**Expected Result:**
- ✅ Error message displayed: "Invalid username or password"
- ✅ Username field retains the value "demo"
- ✅ Password field is cleared
- ✅ Error box has red styling

---

### Test Case 6: Unauthorized Access

**Steps:**
1. Make sure you're logged out (visit `/login` and ensure you're not logged in)
2. Manually try to visit http://localhost:5173/dashboard

**Expected Result:**
- ✅ You should be redirected to `/login`
- ✅ Cannot access protected routes without authentication

---

### Test Case 7: Already Logged In

**Steps:**
1. Log in successfully
2. Manually navigate to http://localhost:5173/login

**Expected Result:**
- ✅ You should be redirected to `/dashboard`
- ✅ Logged-in users cannot access the login page

---

## 🎨 Visual Testing

### Login Page Checklist

Visit `/login` and verify:

- [ ] **Layout:**
  - [ ] Centered card on dark background
  - [ ] "Nexus Trade" logo at top (Nexus in cyan)
  - [ ] "Crypto Futures Portfolio Manager" subtitle

- [ ] **Form Elements:**
  - [ ] Username field with User icon
  - [ ] Password field with Lock icon
  - [ ] "Sign In" button with cyan background
  - [ ] Demo credentials box at bottom

- [ ] **Colors:**
  - [ ] Background: Very dark (#0a0b0d)
  - [ ] Card: Slightly lighter dark (#16181c)
  - [ ] Border: Subtle gray (#2d2f36)
  - [ ] Accent: Cyan/teal (#00ffbd)
  - [ ] Text: White

- [ ] **Interactions:**
  - [ ] Input fields highlight with cyan border on focus
  - [ ] Button shows loading spinner when submitting
  - [ ] Error messages appear in red box

---

### Dashboard Page Checklist

Visit `/dashboard` (after logging in) and verify:

- [ ] **Header:**
  - [ ] "Nexus Trade" branding (left side)
  - [ ] "Welcome back, demo!" message
  - [ ] Red "Logout" button (right side)

- [ ] **Stats Cards (3 cards):**
  - [ ] Total Active Capital ($0) - cyan icon
  - [ ] Deployed Strategies (0) - blue icon
  - [ ] Strategy Drafts (0) - yellow icon

- [ ] **Welcome Section:**
  - [ ] "Welcome to Nexus Trade" heading
  - [ ] Description text
  - [ ] "Create Portfolio" button (cyan)
  - [ ] "View Markets" button (outlined)

- [ ] **Responsive Design:**
  - [ ] Stats cards stack on mobile (< 768px)
  - [ ] Cards display in row on desktop (≥ 768px)

---

## 🔍 Browser DevTools Checks

### Cookies Inspection

1. Open DevTools (F12)
2. Go to **Application** tab (Chrome) or **Storage** tab (Firefox)
3. Select **Cookies** → `http://localhost:5173`

**Verify:**
- [ ] `auth_token` cookie exists after login
- [ ] `refresh_token` cookie exists after login
- [ ] Both have `Path: /`
- [ ] Both have `HttpOnly: true`
- [ ] Both have `SameSite: Strict`
- [ ] Cookies are deleted after logout

### Network Tab

1. Open DevTools → **Network** tab
2. Perform login

**Verify:**
- [ ] POST request to `/login` with form data
- [ ] Response: 303 redirect to `/dashboard`
- [ ] `Set-Cookie` headers in response

### Console

**Verify:**
- [ ] No JavaScript errors in console
- [ ] No 404 errors for assets
- [ ] No CORS errors

---

## 🛠️ Build & TypeScript Checks

### TypeScript Check

```bash
npm run check
```

**Expected Output:**
```
Loading svelte-check in workspace: d:\work\trading_webapp
Getting Svelte diagnostics...

✅ svelte-check found 0 errors and 0 warnings
```

---

### Production Build

```bash
npm run build
```

**Expected:**
- ✅ Build completes without errors
- ✅ Output in `build/` directory
- ✅ No TypeScript errors
- ✅ No Svelte compilation errors

---

### Preview Production Build

```bash
npm run preview
```

**Test:**
- Repeat all test cases above on the preview server
- Verify functionality is identical to dev mode

---

## 📱 Responsive Design Testing

### Desktop (≥ 1024px)
- [ ] Stats cards in horizontal row
- [ ] Login card centered, max-width applied
- [ ] All text readable
- [ ] Proper spacing

### Tablet (768px - 1023px)
- [ ] Stats cards in row or 2-column grid
- [ ] Login card responsive
- [ ] Navigation accessible

### Mobile (< 768px)
- [ ] Stats cards stack vertically
- [ ] Login card full-width with padding
- [ ] Form inputs full-width
- [ ] Buttons full-width
- [ ] Touch targets adequate size

**Test in Chrome DevTools:**
1. Open DevTools (F12)
2. Click device toolbar icon (Ctrl+Shift+M)
3. Test with different device presets

---

## 🌐 Browser Compatibility

Test in the following browsers:

- [ ] **Chrome** (latest)
- [ ] **Firefox** (latest)
- [ ] **Edge** (latest)
- [ ] **Safari** (if available)

**Verify:**
- [ ] Layout consistent
- [ ] Authentication works
- [ ] Cookies set correctly
- [ ] No console errors

---

## 🐛 Troubleshooting

### Issue: "Cannot find module" errors

**Solution:**
```bash
npm install
```

---

### Issue: Port 5173 already in use

**Solution 1:** Kill the existing process

**Windows:**
```bash
netstat -ano | findstr :5173
taskkill /PID <PID> /F
```

**Linux/Mac:**
```bash
lsof -ti:5173 | xargs kill -9
```

**Solution 2:** Change port in `vite.config.ts`:
```typescript
export default defineConfig({
  server: {
    port: 3000  // Change to any available port
  }
});
```

---

### Issue: Cookies not setting

**Possible Causes:**
1. Browser privacy settings blocking cookies
2. Using IP address instead of localhost
3. Mixed HTTP/HTTPS issues

**Solutions:**
- Use `http://localhost:5173` (not `http://127.0.0.1:5173`)
- Check browser cookie settings
- Allow cookies for localhost
- Clear browser cache and cookies

---

### Issue: Tailwind styles not applying

**Solution:**
```bash
# Stop the dev server (Ctrl+C)
# Delete node_modules/.vite cache
rm -rf node_modules/.vite
# Restart
npm run dev
```

---

### Issue: TypeScript errors about environment variables

**Solution:**
Ensure `.env` file exists with:
```env
JWT_SECRET=super-secret-jwt-key-for-nexus-trade-platform-min-32-chars
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

### Issue: 404 on refresh (production build)

**Cause:** SvelteKit uses client-side routing

**Solution:** Configure your web server (Nginx, Apache) to serve `index.html` for all routes

---

## ✅ Success Criteria

Before proceeding to Phase 3, ensure all of the following are met:

### Functionality
- [ ] Login with demo credentials works
- [ ] Dashboard loads after login
- [ ] Logout clears session
- [ ] Protected routes redirect when not authenticated
- [ ] Session persists on page refresh
- [ ] Invalid credentials show error

### Visual Design
- [ ] Dark theme applied correctly
- [ ] Cyan accent color (#00ffbd) on buttons and links
- [ ] Icons display properly
- [ ] Typography is clear and readable
- [ ] Responsive layout on mobile/tablet/desktop

### Technical
- [ ] TypeScript check passes (0 errors)
- [ ] Production build completes
- [ ] No console errors
- [ ] Cookies set with HttpOnly and SameSite
- [ ] No accessibility warnings

### Performance
- [ ] Page loads quickly (< 2s on dev server)
- [ ] No layout shift on page load
- [ ] Smooth transitions and animations

---

## 📊 Test Results Template

Use this template to record your test results:

```
# Test Results - [Date]

## Environment
- Browser:
- OS:
- Node Version:
- npm Version:

## Test Cases
- [ ] Test Case 1: Login Flow - PASS/FAIL
- [ ] Test Case 2: Protected Routes - PASS/FAIL
- [ ] Test Case 3: Logout Flow - PASS/FAIL
- [ ] Test Case 4: Session Persistence - PASS/FAIL
- [ ] Test Case 5: Invalid Credentials - PASS/FAIL
- [ ] Test Case 6: Unauthorized Access - PASS/FAIL
- [ ] Test Case 7: Already Logged In - PASS/FAIL

## Visual Testing
- [ ] Login Page - PASS/FAIL
- [ ] Dashboard Page - PASS/FAIL
- [ ] Responsive Design - PASS/FAIL

## Build Checks
- [ ] TypeScript Check - PASS/FAIL
- [ ] Production Build - PASS/FAIL

## Issues Found
1.
2.
3.

## Notes

```

---

## 🎯 Next Steps

After all tests pass, you're ready to continue with:

1. **API Client Layer** - Typed HTTP client for Spring Boot backend
2. **TypeScript Types** - Map backend DTOs (Portfolio, MarketData, Forecasting)
3. **Base UI Components** - Button, Input, Card, Modal components
4. **Layout Components** - Sidebar and Header navigation

---

## 📞 Support

If you encounter issues not covered in this guide:

1. Check the main `README.md` for setup instructions
2. Review the implementation plan: `C:\Users\nguye\.claude\plans\swirling-swinging-axolotl.md`
3. Inspect browser console for error messages
4. Check network tab for failed requests

---

**Happy Testing! 🚀**

Last Updated: 2025-12-23
