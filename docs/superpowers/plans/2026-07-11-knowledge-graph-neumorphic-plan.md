# Knowledge Graph Neumorphic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align history heading height and rebuild the knowledge graph workspace with an independent four-card heading and stable large graph canvas.

**Architecture:** Preserve Vue API methods and ECharts graph behavior. Restructure only the graph template and page-prefixed CSS, use shared surface primitives, and enforce the approved layout through the existing UI contract script.

**Tech Stack:** Vue 3, Vite, Element Plus, lucide-vue-next, Apache ECharts.

## Global Constraints

- Keep the graph metrics as three independent cards to the right of the heading card.
- Use 204px as the desktop top-row card height.
- Do not change API paths or graph interactions.
- Do not use gradients or generic cross-page graph classes.

---

### Task 1: Add UI contracts

**Files:**
- Modify: `frontend/scripts/check-ui-contract.mjs`

- [ ] Assert history `min-height: 204px`.
- [ ] Assert graph page-specific hero, stats, toolbar, surface classes, `kg-graph-canvas`, and `shallowRef`.
- [ ] Run `npm.cmd run test:ui` and confirm the first new token fails.

### Task 2: Align history heading

**Files:**
- Modify: `frontend/src/views/history/history-view.css`

- [ ] Change only `.history-hero` minimum height to 204px.
- [ ] Run `npm.cmd run test:ui`.

### Task 3: Rebuild graph layout

**Files:**
- Modify: `frontend/src/views/graph/KnowledgeGraphView.vue`
- Modify: `frontend/src/views/graph/knowledge-graph-view.css`

- [ ] Keep hero and three statistic cards as siblings in the first grid row.
- [ ] Move all controls and actions into the full-width graph toolbar.
- [ ] Rename graph structural classes with the `kg-` prefix and apply shared surface classes.
- [ ] Replace chart `ref` with `shallowRef` and rename the chart container to `kg-graph-canvas`.
- [ ] Preserve all graph requests, ECharts configuration, click handling, zoom, drag, and resize behavior.

### Task 4: Verify and document

**Files:**
- Create: `docs/phase-notes/phase-28-neumorphic-knowledge-graph.md`

- [ ] Run `npm.cmd run test:ui`.
- [ ] Run `npm.cmd run build`.
- [ ] Record modified files, preserved behavior, verification results, and the next-page handoff.
