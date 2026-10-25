# RailReserve — Security Audit (Milestone 6, Step 1)

Date: 2026-10-24 · Scope: entire git history (9 commits), tracked files, working tree.

## Method

- `git log --all --oneline` — listed all commits (local repository, 9 commits, no remotes).
- Pickaxe scan over all commits: `git log --all -G"<secret-patterns>"` for the Supabase
  password, project ref, host, `pooler.supabase`, and `DB_PASSWORD=...` forms.
- Full-history path check: `git log --all --full-history -- .env`.
- Tracked-file check: `git ls-files | grep -i env`.
- Working-tree grep for the credential substrings, excluding `.env` itself, `.git/`, `target/`.

## Findings

| Check | Result |
|---|---|
| Real Supabase password committed at any commit | **No** — string never appears in tracked content |
| Real Supabase project ref / host committed | **No** — only generic template `aws-0-<region>.pooler.supabase.com` in `.env.example`, intentionally |
| `.env` committed at any commit | **No** — never tracked; only `middleware/.env.example` is tracked; `.env` is gitignored (patterns also present in all three tier folders) |
| Working-tree secret leaks outside `.env` | **None** |

## One near-match in history (open question, no action taken)

- Commit `2ef4f77` (Milestone 5) added a JUnit test sample string `"Command@1806200"` in
  `middleware/src/test/java/com/railreserve/util/PasswordUtilTest.java`. It is a near
  look-alike of the real deployment password (`Commando@1806200`) but is **not** the real
  credential. It was removed in the follow-up commit `9f11380`, so current code contains only
  neutral samples — however the older blob remains reachable in git history.

  Decision recorded by the project owner: keep history as-is, do not rewrite commits, do not
  push. Recommended hygiene: the deployment password used during development is treated as
  compromised-prone and can be rotated from the Supabase dashboard at any time without any
  code change (it lives only in the local untracked `.env`).

## Status

- Current tree is clean of secrets; no history rewrite performed (per owner instruction).
