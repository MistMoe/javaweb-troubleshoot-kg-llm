# Knowledge And Favorites Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify the knowledge base and favorites pages under the approved data-page heading variant while preserving all business behavior.

**Architecture:** Both pages keep their existing Vue data flow and API calls. Template-only structure changes move metrics into each page heading and actions into a dedicated toolbar; page-prefixed CSS provides isolated neumorphic styling without global selector collisions.

**Tech Stack:** Vue 3, Vite, Element Plus, lucide-vue-next, Apache ECharts, scoped-by-prefix CSS.

## Global Constraints

- Do not modify login or registration views.
- Do not change API paths, request parameters, permissions, favorites, review, or Neo4j behavior.
- Do not add dependencies, gradients, colored full-card edges, or horizontal overflow.

---

### Task 1: Add layout contracts

**Files:**
- Modify: `frontend/scripts/check-ui-contract.mjs`

**Interfaces:**
- Consumes: existing source files as UTF-8 strings.
- Produces: assertions for `knowledge-overview`, `knowledge-toolbar`, `favorites-overview`, `favorites-toolbar`, and page-prefixed card classes.

- [ ] Add missing-token assertions for the approved structures.
- [ ] Run `npm.cmd run test:ui` and confirm failure on the first missing token.

### Task 2: Rebuild the knowledge heading

**Files:**
- Modify: `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- Modify: `frontend/src/views/knowledge/knowledge-base-view.css`

**Interfaces:**
- Consumes: existing computed counts, permissions, event handlers, and API methods.
- Produces: a single data overview heading plus a differentiated action toolbar.

- [ ] Keep the three metrics as independent `knowledge-stat-card` siblings and align their height with `knowledge-hero-card`.
- [ ] Move existing actions into `knowledge-toolbar` and preserve all conditions and click handlers.
- [ ] Remove the old one-column statistic cards and update grid placement.
- [ ] Run the UI contract and production build.

### Task 3: Redesign the favorites page

**Files:**
- Modify: `frontend/src/views/favorites/FavoriteCasesView.vue`
- Modify: `frontend/src/views/favorites/favorite-cases-view.css`

**Interfaces:**
- Consumes: existing favorites list, computed metrics, selection, filtering, navigation, and removal handlers.
- Produces: a data overview heading, action toolbar, isolated list and detail panels.

- [ ] Move all metrics into `favorites-overview`.
- [ ] Add `favorites-toolbar` with refresh and knowledge navigation actions.
- [ ] Rename generic structural classes to `favorites-*` and apply the shared surface classes.
- [ ] Reduce empty-state height and style active list rows with the pressed surface state.
- [ ] Run the UI contract and production build.

### Task 4: Document and verify

**Files:**
- Create: `docs/phase-notes/phase-27-data-heading-knowledge-favorites.md`

**Interfaces:**
- Consumes: final implementation and command results.
- Produces: implementation record, preserved behavior, verification results, and next-page handoff.

- [ ] Run `npm.cmd run test:ui`.
- [ ] Run `npm.cmd run build`.
- [ ] Record exact files, behavior boundaries, and remaining visual review needs.
