# Archive

Superseded code kept for reference only. **Nothing here is built, run, tested, or
referenced by the live projects.** Do not treat anything in this folder as a source of
truth for API contracts, UI behaviour, or architecture.

---

## `ninemo-frontend/` — React Native app (archived 2026-08-11)

The original React Native + TypeScript mobile client (13 screens). Replaced by
**`ninemo-mobile/`** (Kotlin Multiplatform + Compose, Android-first) after the native
pivot — see `docs/Cross_Platform_Strategy.md` §"ninemo-mobile/ ← new top-level dir
(replaces ninemo-frontend/)" and `docs/Android_Native_Migration_Analysis.md`.

Last touched before 2026-07. Zero references from `ninemo-backend/` or `ninemo-mobile/`.

**Do not use `src/types/api.ts` as an API contract reference.** Those interfaces were
written from prose specs rather than from the real Spring controllers, and every one of
them turned out to be wrong in some way. The same invented types were ported into the KMP
scaffold and had to be rewritten in all six mobile phases — see IS-020, IS-022, IS-024,
IS-026, IS-029, IS-030 in `docs/Issue_Tracker.md`. The rule that came out of it:

> Read the actual Spring controller / DTO / entity source — and the service code when a
> DTO field is an untyped `Map` — before porting any contract.

The layered architecture the RN app established (services → hooks → screens → store) was
deliberately carried over to KMP as repositories → ViewModels → composables, so the
*shape* of the code still maps. The field names do not.

For UI reference use `docs/UI_Design.md`, not this code.

### `ninemo-frontend/CLAUDE.md`

Was `docs/CLAUDE.md` — the React Native architecture rules, auto-loaded into every session
as project instructions long after the RN app stopped being built. Moved here 2026-08-11
because it described a stack nobody edits. Its *principles* (thin client, strict layer
separation, backend owns all medical logic, contracts mirror backend DTOs) survive in the
"Mobile — `ninemo-mobile/`" section of the root `CLAUDE.md`, restated for KMP.
