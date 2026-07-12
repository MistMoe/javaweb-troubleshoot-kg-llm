# Management Pages Neumorphic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild user and role management with aligned 204px management headings and a single nested three-metric summary card on the right.

**Architecture:** Preserve Vue data flow and APIs. Add shared surface classes in templates, keep scoped page CSS, and adjust grid placement and component material without changing business handlers.

**Tech Stack:** Vue 3, Vite, Element Plus, lucide-vue-next, scoped CSS.

## Global Constraints

- Heading and summary card height is 204px on desktop.
- Summary metrics stay inside one right-side card.
- No gradients, colored full-card top borders, new dependencies, or API changes.

---

### Task 1: Add UI contracts

**Files:**
- Modify: `frontend/scripts/check-ui-contract.mjs`

- [ ] Require page-specific management hero and summary classes, shared surfaces, nested metric structures, 24px grids, and 204px heights.
- [ ] Run `npm.cmd run test:ui` and confirm failure.

### Task 2: Optimize user management

**Files:**
- Modify: `frontend/src/views/users/UserManagementView.vue`
- Modify: `frontend/src/views/users/user-management-view.css`

- [ ] Apply shared surface and action classes.
- [ ] Align hero and summary to 204px and keep three nested metrics.
- [ ] Make filters full width and remove colored card edges.
- [ ] Restyle user rows, role boundary, form controls, and responsive layout without changing handlers.

### Task 3: Optimize role management

**Files:**
- Modify: `frontend/src/views/roles/RoleManagementView.vue`
- Modify: `frontend/src/views/roles/role-management-view.css`

- [ ] Apply shared surface and action classes.
- [ ] Align hero and nested summary to 204px.
- [ ] Remove colored card edges and restyle role, permission, flow, and boundary content.
- [ ] Preserve fixed role blueprints, permission rows, and role loading.

### Task 4: Verify and document

**Files:**
- Create: `docs/phase-notes/phase-29-neumorphic-management-pages.md`

- [ ] Run `npm.cmd run test:ui`.
- [ ] Run `npm.cmd run build`.
- [ ] Record behavior boundaries and next-page handoff.
