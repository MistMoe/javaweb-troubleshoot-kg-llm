# Knowledge Submission Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a lightweight knowledge-submission workflow where users submit troubleshooting experience, DeepSeek drafts structured case fields, and ADMIN/MAINTAINER approve it into the fault knowledge base.

**Architecture:** Add a backend submission module beside `errorcase`, backed by a new `error_case_submission` table. Reuse `LlmAdviceService` to generate candidate case fields and reuse `ErrorCaseRepository` to approve submissions into the existing `error_case` table. Extend the existing knowledge base page with a submission dialog and reviewer panel, without creating a separate page.

**Tech Stack:** Spring Boot 3, JdbcTemplate, MySQL/H2 tests, DeepSeek-compatible LLM service, Vue 3, Element Plus, Axios.

## Global Constraints

- Login/register visuals must not be modified.
- Review permission belongs to `ADMIN` and `MAINTAINER`; normal `USER` can submit but cannot approve.
- Approved submissions become normal `error_case` rows and can be synced to Neo4j through the existing import button.
- Personal profile and favorite pages are future work; this task must preserve submitter metadata and keep case IDs stable so those pages can link to submissions/cases later.

---

### Task 1: Backend Submission API

**Files:**
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmission.java`
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionRequest.java`
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionDraft.java`
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionRepository.java`
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionService.java`
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionController.java`
- Modify: `backend/src/main/java/com/mistmoe/troubleshoot/llm/LlmAdviceService.java`
- Modify: `backend/src/test/resources/schema.sql`
- Modify: `backend/src/test/resources/data.sql`
- Test: `backend/src/test/java/com/mistmoe/troubleshoot/submission/KnowledgeSubmissionControllerTest.java`

**Interfaces:**
- Produces `POST /api/submissions/draft`
- Produces `POST /api/submissions/create`
- Produces `GET /api/submissions/list?status=PENDING`
- Produces `GET /api/submissions/{id}`
- Produces `PUT /api/submissions/{id}/approve`
- Produces `PUT /api/submissions/{id}/reject`
- Reuses `ErrorCaseRepository.create(ErrorCaseRequest)` on approve.

**Steps:**
- [ ] Write `KnowledgeSubmissionControllerTest` covering draft generation, create/list/detail, approve into `error_case`, reject with comment.
- [ ] Run the targeted test and verify it fails because the controller does not exist.
- [ ] Add records, repository, service, controller, and test schema.
- [ ] Add `LlmAdviceService.generateCaseDraft(...)` with deterministic fallback fields when LLM is disabled.
- [ ] Run the targeted test and verify it passes.

### Task 2: Frontend Knowledge Page Integration

**Files:**
- Modify: `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- Modify: `frontend/src/views/knowledge/knowledge-base-view.css`

**Interfaces:**
- Consumes all `/api/submissions/*` routes from Task 1.
- Uses current user roles to show the review panel only to `ADMIN` and `MAINTAINER`.

**Steps:**
- [ ] Add `投稿经验` button visible to all logged-in users.
- [ ] Add `待审核 N` button visible only to `ADMIN` and `MAINTAINER`.
- [ ] Add a large submission dialog with raw input on the left and editable AI draft fields on the right.
- [ ] Add review panel with list, status filter, details, approve/reject actions.
- [ ] On approve, refresh both submissions and knowledge base cases.
- [ ] Keep the existing knowledge base layout and cool white neumorphic style.

### Task 3: SQL, Docs, Verification

**Files:**
- Create: `docs/sql/2026-07-10-error-case-submission.sql`
- Create: `docs/phase-notes/phase-18-knowledge-submission-review.md`
- Modify: `README.md`

**Steps:**
- [ ] Add MySQL migration SQL for `error_case_submission`.
- [ ] Add phase notes explaining the knowledge closed loop.
- [ ] Update README feature list and API list.
- [ ] Run backend targeted test.
- [ ] Run frontend build.
