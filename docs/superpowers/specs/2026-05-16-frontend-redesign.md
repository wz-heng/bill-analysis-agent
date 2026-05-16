# Frontend Redesign — 账单分析助手

**Date:** 2026-05-16  
**Status:** Approved

## Overview

Complete visual redesign of the Vue 3 frontend for the bill analysis agent. The existing functionality (CSV input, question input, `/api/bills/analyze` API call, result display) is preserved unchanged. Only the visual layer is rewritten.

## Aesthetic Direction

**暗色编辑风 (Dark Editorial Luxury)**

Inspired by Financial Times meets Bloomberg Terminal — refined typographic authority over dark, warm backgrounds with a single amber/gold accent color.

- **Background:** Deep warm near-black `#0c0b09`
- **Surface/Card:** `#161412`, `#111008`
- **Border:** `#282420` (subtle warm gray)
- **Accent:** Amber gold `#c9a028` — used sparingly: section numbers, active states, CTA button
- **Text primary:** Warm off-white `#e2dbd0`
- **Text secondary/muted:** `#8a8278`, `#504840`
- **Success (income/valid):** Muted green `#5a9a6a`
- **Warning/anomaly:** Same amber gold

## Typography

| Role | Font | Weight | Notes |
|------|------|--------|-------|
| Brand title | Cormorant Garamond | 500 | 20px, editorial presence |
| Result section headings | Cormorant Garamond | 500 | 14px, serif within prose |
| UI labels / body | DM Sans | 300–500 | Clean, modern sans |
| Section numbers (01/02/03) | JetBrains Mono | 400 | 9px, gold color |
| CSV textarea / code | JetBrains Mono | 400 | 9.5–11px |
| Status / metadata | JetBrains Mono | 400 | Small caps feel |

All fonts loaded from Google Fonts via `index.html`.

## Layout

**Top-down flow (上下流式)**

```
┌─────────────────────────────────────────────────────┐
│  Header: brand + status dot                         │
├─────────────────────────────────────────────────────┤
│  [ 01 账单数据 CSV ]    │  [ 02 分析问题 + CTA ]    │
│  (wider, ~55%)         │  (narrower, ~45%)          │
├─────────────────────────────────────────────────────┤
│  [ 03 分析结果 — full width ]                       │
└─────────────────────────────────────────────────────┘
│  Footer                                             │
```

- Input row: `grid-template-columns: 1.3fr 1fr` — CSV wider, question narrower
- Result: always full width below the input row
- Result panel is sticky-aware but not sticky (result scrolls naturally)
- Mobile: single column stack

## Components

### Header
- Brand icon (chart bars SVG) in a bordered box with gold stroke
- `账单分析助手` in Cormorant Garamond 20px
- `AI Financial Intelligence` tagline in uppercase, letter-spaced, dim color
- Animated pulse dot + `DeepSeek AI` label in JetBrains Mono

### Section Cards
Each card has:
- `card-hd`: section number (`01`/`02`/`03`) in gold mono + label in DM Sans, card actions right-aligned
- Body content
- Optional `card-ft`: dim metadata left, gold count/timing right
- Hover: border subtly brightens from `#282420` → `#383430`

### CSV Card (01)
- Header: "示例数据" ghost button + "上传 CSV" gold-tinted button
- Body: dark `#0f0e0c` background, JetBrains Mono 9.5px, dim color, drag-and-drop overlay
- Footer: format hint left, record count right in gold mono

### Question Card (02)
- Suggestion chips: pill shape, border only; active chip gets gold tint + border
- Question textarea: transparent background, DM Sans 13px warm text
- Analyze button: full-width inside card, `linear-gradient(135deg, #c9a028, #8a6810)`, dark text `#1a1200`, gold glow on hover

### Result Panel (03)
States:
1. **Empty:** centered layout, large Cormorant Garamond "等待分析" heading, 3 capability items (Σ / ↗ / ⚑) with gold symbols separated by hairlines
2. **Loading:** shimmer skeleton lines (`#1e1c18` → `#282420` gradient animation)
3. **Error:** card border tinted red, error message in red-tinted box
4. **Result:** header has validation badge (green "数据已验证" / amber "存在疑问"), prose body with Cormorant Garamond section h4s, scrollable max-height

### Footer
Single line, uppercase, extreme letter-spacing, very dim color. `Spring AI · DeepSeek · Ground Truth 校验`

## Texture & Atmosphere
- Grain overlay via SVG `feTurbulence` data URL in `body::before`, fixed position, `pointer-events: none`, opacity ~0.03–0.05
- Subtle radial gradient on background (warm amber glow at ~20% opacity near top-left)

## Technical Notes

- **Framework:** Vue 3 + Tailwind CSS (layout utilities) + scoped `<style>` for component styles
- **No Tailwind for theme colors** — use CSS custom properties on `.app` for all color tokens
- **Prose styles** use `:deep()` selector to pierce scoped CSS on v-html content
- **API contract unchanged:** `POST /api/bills/analyze` → `{ answer, groundTruthValid, validationMessage, processingTimeMs }`
- `formatAnswer()` logic preserved; markdown `# headings` output as `<h4>` (not the old `<p class="font-semibold">`) so Cormorant Garamond `:deep(h4)` styling applies
- `index.html` gets Google Fonts preconnect + link tags
- `style.css` reduced to Tailwind directives + body background only; all component styles in `App.vue`
- Responsive breakpoint at 768px: input row becomes single column

## Files to Modify

| File | Change |
|------|--------|
| `frontend/index.html` | Add Google Fonts (Cormorant Garamond, DM Sans, JetBrains Mono) |
| `frontend/src/style.css` | Strip to Tailwind directives + grain texture on body |
| `frontend/src/App.vue` | Complete template + style rewrite; script logic unchanged |
