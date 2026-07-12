# Profile Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a flat, two-column personal account workspace where all three roles can update their own nickname, email, and password.

**Architecture:** Add self-service endpoints inside the existing auth domain so the bearer session, rather than a client-supplied user ID, selects the account. Replace the static profile card grid with one continuous Vue workspace and propagate successful profile updates back to `App.vue`.

**Tech Stack:** Spring Boot 3, JdbcTemplate, MockMvc/JUnit 5, Vue 3 Composition API, Axios, Element Plus, scoped page CSS.

## Global Constraints

- Do not modify login or registration pages.
- Keep username, status, and roles read-only.
- Use flat cool-white surfaces, thin separators, and restrained blue accents; no neumorphic shadows or gradients.
- Preserve existing analysis, favorites, and submission statistics.
- All `ADMIN`, `MAINTAINER`, and `USER` accounts share the same self-service behavior.

---

### Task 1: Auth self-service profile API

**Files:**
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/auth/ProfileUpdateRequest.java`
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/auth/PasswordChangeRequest.java`
- Create: `backend/src/main/java/com/mistmoe/troubleshoot/auth/PasswordChangeResponse.java`
- Modify: `backend/src/main/java/com/mistmoe/troubleshoot/auth/UserProfile.java`
- Modify: `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthRepository.java`
- Modify: `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthService.java`
- Modify: `backend/src/main/java/com/mistmoe/troubleshoot/auth/AuthController.java`
- Test: `backend/src/test/java/com/mistmoe/troubleshoot/auth/AuthControllerTest.java`

**Interfaces:**
- Consumes: bearer token stored by `AuthService.login`.
- Produces: `PUT /api/auth/profile` returning updated `UserProfile`; `PUT /api/auth/password` returning `{ "message": "密码修改成功" }`.

- [ ] **Step 1: Add failing MockMvc tests**

Add tests for successful nickname/email update, invalid email, wrong current password, successful password change, and missing bearer token. Obtain the bearer token by logging in through `/api/auth/login`; annotate the test class with `@Transactional` so account changes roll back.

- [ ] **Step 2: Run the focused tests and confirm failure**

Run: `..\tools\apache-maven-3.9.16\bin\mvn.cmd -Dtest=AuthControllerTest test`

Expected: new `PUT` endpoint tests fail with 405 or 404.

- [ ] **Step 3: Implement request records and repository mutations**

Add `updateProfile(Long id, String nickname, String email)`, `passwordMatches(Long id, String password)`, and `updatePassword(Long id, String password)` to `AuthRepository`. Use parameterized SQL and update `update_time`.

- [ ] **Step 4: Implement service validation and session refresh**

Resolve the token from the authorization header, validate email with a conservative pattern, trim nullable fields, enforce a six-character new password, verify the current password, and replace the session's immutable `UserProfile` after a profile update.

- [ ] **Step 5: Expose the two controller endpoints**

Return 401 when the bearer token is absent/invalid and 400 with `ErrorResponse` for validation or current-password failures.

- [ ] **Step 6: Run focused tests**

Run: `..\tools\apache-maven-3.9.16\bin\mvn.cmd -Dtest=AuthControllerTest test`

Expected: all `AuthControllerTest` tests pass.

### Task 2: Profile UI contract

**Files:**
- Modify: `frontend/scripts/check-ui-contract.mjs`

**Interfaces:**
- Consumes: `ProfileView.vue` and `profile-view.css` source tokens.
- Produces: a failing contract for the new continuous workspace before implementation.

- [ ] **Step 1: Register the profile files in the contract script**

Read `src/views/profile/ProfileView.vue` and `src/views/profile/profile-view.css`.

- [ ] **Step 2: Require the new structure**

Require tokens `profile-workspace`, `profile-identity-rail`, `profile-editor`, `profile-basic-section`, `profile-security-section`, `profile-updated`, `saveProfile`, and `changePassword`. Require CSS tokens `grid-template-columns: 300px minmax(0, 1fr)`, `box-shadow: none`, and `.profile-section + .profile-section`.

- [ ] **Step 3: Run and confirm failure**

Run: `npm.cmd run test:ui`

Expected: failure reporting the first missing profile token.

### Task 3: Flat two-column profile workspace

**Files:**
- Replace: `frontend/src/views/profile/ProfileView.vue`
- Replace: `frontend/src/views/profile/profile-view.css`
- Modify: `frontend/src/App.vue`

**Interfaces:**
- Consumes: `PUT /api/auth/profile`, `PUT /api/auth/password`, and the existing stats endpoints.
- Produces: `profile-updated(updatedUser)` emitted after a successful basic-profile save.

- [ ] **Step 1: Build the continuous page structure**

Create one `profile-workspace` with `profile-identity-rail` on the left and `profile-editor` on the right. Keep role badges, three compact statistics, recent activity, and text shortcuts, but organize them as sections separated by rules rather than cards.

- [ ] **Step 2: Implement basic profile editing**

Initialize a reactive form from `currentUser`, validate optional email, submit with bearer authorization, show loading/success/error states, and emit `profile-updated` with the response.

- [ ] **Step 3: Implement password change**

Require all three fields, ensure new and confirmation passwords match and are at least six characters, call the password endpoint, then clear all password fields after success.

- [ ] **Step 4: Synchronize the shell user state**

Add `@profile-updated="currentUser = $event"` to the `ProfileView` usage in `App.vue` so the top bar updates immediately.

- [ ] **Step 5: Apply flat responsive styling**

Use a 300px identity rail and flexible editor on desktop, switch to one column below 900px, use `box-shadow: none`, 1px separators, and no gradients. Preserve natural vertical scrolling and avoid horizontal overflow.

- [ ] **Step 6: Run the UI contract and build**

Run: `npm.cmd run test:ui`

Expected: UI contract passes.

Run: `npm.cmd run build`

Expected: Vite build succeeds; existing chunk-size warnings are acceptable.

### Task 4: End-to-end verification and phase note

**Files:**
- Create: `docs/phase-notes/phase-30-flat-profile-workspace.md`

**Interfaces:**
- Consumes: completed backend and frontend behavior.
- Produces: implementation record and next-step handoff.

- [ ] **Step 1: Run backend auth tests**

Run: `..\tools\apache-maven-3.9.16\bin\mvn.cmd -Dtest=AuthControllerTest test`

Expected: zero failures and zero errors.

- [ ] **Step 2: Run frontend checks**

Run: `npm.cmd run test:ui`

Run: `npm.cmd run build`

Expected: both commands succeed.

- [ ] **Step 3: Write the phase note**

Document the flat workspace layout, editable fields, endpoint paths, password rules, session refresh behavior, verification commands, and the deliberate exclusions of avatar upload and password hashing migration.

- [ ] **Step 4: Report manual visual checks**

Ask the user to verify desktop alignment, narrow-screen stacking, profile save reflection in the top bar, and login with the changed password because automated browser control is unavailable for this local page.

## Execution Note

Git metadata in this workspace is currently unavailable, so the implementation will preserve the existing backup and skip commit steps rather than attempting repository repair during this UI task.
