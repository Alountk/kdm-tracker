# Architecture

The file tree lives in the [README](../README.md#project-structure). This page holds what the tree
cannot say: who may depend on whom, and how we comment.

## Layers

```text
app-android ──▶ data ──▶ core ◀── server
     └──────────────────▶ core
```

| Module | Owns | Must not contain |
|---|---|---|
| `core` | Domain types, rules, use cases, ports (`CampaignStore`, `SyncApi`). Pure Kotlin Multiplatform. | Android, Spring, SQL, I/O |
| `data` | SQLDelight implementations of the `core` persistence and sync ports. | Game rules |
| `app-android` | Compose UI, `ViewModel`, the controller that mirrors `core` results into `UiState`. | Game rules, SQL |
| `server` | Spring sync hub and web dashboard. | Game rules that `core` already has |

Rule of thumb: if a change decides *what the game allows*, it belongs in `core/rules` or
`core/usecase`. The layers above it only call and display.

## Comment convention

Code says **what** it does. A comment is for what the code cannot say.

**Write a comment when:**

- there is a non-obvious *why* (a workaround, a constraint, a surprising choice);
- there is an invariant or ordering the next editor could break (for example "the hub call must
  happen before any local write");
- a public member's name and signature do not tell a caller what they need (units, nullability
  meaning, side effects, failure behavior).

**Do not write:**

- a restatement of the code (`/** Moves the monster. */ fun moveMonster()`);
- process history: ODD task ids (`S3`, `T2`), PR numbers, "Ruling AJ", "Review Focus 3", design-tool
  names, who asked for it. That belongs in `odd/tasks/`, the PR, or the commit message. If a durable
  decision is needed to understand the code, link its document by path;
- architecture justifications on each class ("no business rules live here"). This page says it once;
- a changelog (`previously this did X`). Git has it.

**Size:**

- KDoc is 1-3 lines by default. Past that, ask whether the code should be split or renamed instead
  of explained.
- Private members get a comment only for a real *why*.
- Prefer a better name or a small extracted function over a comment that labels a block.

Test the rule on any comment: *would a reader who knows Kotlin and this page still be surprised
without it?* If not, delete it.
