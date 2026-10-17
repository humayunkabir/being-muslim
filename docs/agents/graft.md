# Graft — repo context graph

How to use the `graft` code-context graph in this repo. `graft/` holds small
linked markdown nodes (per system) carrying exact `file:line` spans, kept in sync
with the code. It is a **local, regenerable cache and is not committed**
(`.gitignore`); rebuild it per clone with `graft build`.

For any task — understanding how something works, finding where code lives, or
scoping a change — get context from the graph before grepping or opening source
files. Re-ask freely; it is cheap. New here? Run `graft map` first for a
token-budgeted orientation (dir clusters, hubs, hotspots), no LLM, no key.

- `graft ask "<question>" --source` → ranked nodes with the relevant code spans
  inlined (each hit's ≤8-line crux; `--full` for whole definitions). The top node
  is usually the answer — cite its `covers:` spans and edit straight from
  `--source`. Ranked results are top-N, so for exhaustive questions ("every
  caller of X") use `graft grep "<literal>"` instead (exhaustive, grouped by
  enclosing symbol), falling back to `grep -rn` only for unindexed files.
- `graft skeleton <file>` → every definition's signature + span; the cheapest way
  to skim a file's API surface.
- `graft callers <symbol>` → precomputed edges: who calls this. Add
  `--direction out` for callees, or `--depth N` to walk transitively for a blast
  radius. Use this directly for structural questions.
- `graft/INDEX.md` lists every node; follow the links.
- In a repo with several sub-projects, hits carry `[scope/]` labels naming where
  they came from; narrow with `graft ask "<task>" --in <scope>/` once you know
  where you are working.

If a span is truncated ("+N more lines"), open the file at that exact range
before finalizing. Only open source files when a node genuinely lacks a detail,
and then at the exact `file:line` it points to — never re-read whole files.

After big code changes, refresh the graph with `graft build` (deterministic, no
API key, $0). `graft check` fails if it is stale (for CI).
