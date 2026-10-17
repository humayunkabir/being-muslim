# Agent skills (vendored)

This repository vendors a curated set of agent skills so that OpenCode discovers
them automatically. They are prompt documents, not code: no build step, no
runtime dependency, and no effect on the iOS or Android builds.

## What is installed

| Item | Value |
| --- | --- |
| Upstream | [mattpocock/skills](https://github.com/mattpocock/skills) — "Skills For Real Engineers" |
| Docs | <https://www.aihero.dev/skills> |
| Version | `1.3.1` (`.claude-plugin/plugin.json`) |
| Commit | `49dd158d1076134a641b33efb035946536778336` (2026-10-09) |
| License | MIT, (c) 2026 Matt Pocock — see `.agents/skills/LICENSE.mattpocock-skills` |
| Skills | 27 |

The set is exactly the `skills` array published in the upstream plugin manifest.
Upstream's `skills/misc/`, `skills/in-progress/`, and `skills/deprecated/`
buckets are deliberately not vendored (`misc/` is frozen and unmaintained;
`in-progress/` is beta and may change or disappear without warning).

## Layout

```text
.agents/skills/<skill-id>/SKILL.md   # the skill body; supporting files sit beside it
.opencode/commands/<skill-id>.md     # thin wrapper so /<skill-id> works
```

### Why `.agents/skills/` and not `.opencode/skills/`

`.agents/skills/` is the cross-agent "universal directory". It is where the
official installer puts OpenCode skills — in [vercel-labs/skills][cli], the
`opencode` agent entry is defined as `skillsDir: '.agents/skills'`, so
`npx skills@latest add mattpocock/skills -a opencode` — the command upstream
tells OpenCode users to run — writes there. OpenCode V2 reads it as a project
source, and so do other agents, which keeps this copy usable if Claude Code,
Codex, or Cursor joins the workflow.

[cli]: https://github.com/vercel-labs/skills

**Do not add a duplicate under `.opencode/skills/`.** In OpenCode's precedence
order, `.agents/skills/` is a *compatibility* source that sits below project
`.opencode/skills/`. A skill defined in both places would silently load the
`.opencode/skills/` copy, leaving this one dead and the two drifting apart.

### Why the commands live in `.opencode/`

OpenCode discovers commands only from `~/.config/opencode/commands/` and
`.opencode/commands/` (plus the legacy singular `command/` directory). There is
no `.agents/commands/` source, so the install is split by necessity: skills in
`.agents/`, command wrappers in `.opencode/`.

Confirmed by probe rather than docs alone: a command file placed in
`.claude/commands/` or `.agents/commands/` does not appear in
`GET /api/command`. Skills have a cross-agent home; commands do not, so don't
relocate these to match the `.agents/skills/` convention.

Nothing else in `.agents/skills/` may be a `.md` file at its root — OpenCode
treats every root-level Markdown file in a skills source as a skill, which is
why the licence is stored as `LICENSE.mattpocock-skills` — it is not `.md`, so
the scanner ignores it.

## Portability notes

| Upstream wording | Here |
| --- | --- |
| "the Skill tool" | OpenCode's `skill` tool, called with the skill ID |
| `/<skill-id>` | `.opencode/commands/<skill-id>.md` |
| `agents/openai.yaml` per skill | Not vendored — Codex/OpenAI-harness metadata, unused here |

Upstream writes cross-skill dependencies as "Call the Skill tool with `<id>`"
so they stay harness-neutral, and uses `/name` mentions for material a human
reads — router prose, option tables, and "tell the user to run
`/setup-matt-pocock-skills`". The command wrappers make those mentions typable
here, matching the slash-command UX the skills are written for. They are not
patching broken references: no skill's operative steps depend on a `/name`
resolving.

## Two invocation classes

Upstream splits the set by who may reach for it, and the frontmatter preserves
that split:

- **Model-invoked** — no `disable-model-invocation`; OpenCode advertises these
  and the agent may load them on its own: `tdd`, `code-review`,
  `codebase-design`, `domain-modeling`, `diagnosing-bugs`, `prototype`,
  `research`, `pr`, `wizard`, `grilling`, `writing-for-agents`.
- **User-invoked** — `disable-model-invocation: true`; the agent cannot see them
  and they are reachable only when you type them: `grill-me`, `grill-with-docs`,
  `triage`, `to-spec`, `to-tickets`, `implement`, `implement-spec`,
  `improve-codebase-architecture`, `wayfinder`, `retro`, `handoff`, `teach`,
  `wait-what`, `to-questionnaire`, `ask-matt`, `setup-matt-pocock-skills`.

## Setup state

`/setup-matt-pocock-skills` has run. The configuration it wrote lives in
`docs/agents/`:

- **Issue tracker**: GitHub (`humayunkabir/being-muslim`), driven with the `gh`
  CLI. The remote host `personal.github.com` is an SSH alias for
  `ssh.github.com`, not an enterprise host — `gh` resolves the repo through it
  without `-R`, verified with `gh repo view`.
- **Triage labels**: the five canonical roles, labelled verbatim. Four were
  created on the repo; `wontfix` already existed as a GitHub default.
- **Domain docs**: single-context — one root `GLOSSARY.md`, ADRs in
  `docs/DECISIONS/`, per `docs/ARCHITECTURE.md` §9.

`AGENTS.md` carries a matching `## Agent skills` block pointing at those files.

## Verifying a change

OpenCode exposes the loaded skills and commands over its API. Pass the project
directory explicitly — without it the request resolves against the server's
default location and reports almost nothing:

```bash
opencode api get '/api/skill?location%5Bdirectory%5D='"$PWD"
opencode api get '/api/command?location%5Bdirectory%5D='"$PWD"
```

Expect 29 skills (27 vendored + the built-in `opencode` and `report`) and 29
commands (27 wrappers + built-in `init` and `review`). Each skill entry reports
its resolved `path` and whether `autoinvoke` is off, which is how the two
invocation classes above can be confirmed rather than assumed.

## Updating

The vendored copy is editable and yours to change — upstream intends that. It
does not self-update.

```bash
git clone --depth 1 https://github.com/mattpocock/skills.git /tmp/mattpocock-skills
# review upstream's .claude-plugin/plugin.json skills array, then re-copy changed
# skill directories into .agents/skills/ and refresh this doc's version + commit
```

Re-copying overwrites local edits. If you start adapting the skills heavily,
prefer editing in place under `.agents/skills/` and treating upstream only as a
reference — and consider recording substantial deviations here so a future
update does not silently revert them.

## Related

- `opencode.json` — project OpenCode config (MCP servers; unrelated to skills).
- `AGENTS.md` — repository engineering rules; the `## Agent skills` block is
  added there by `/setup-matt-pocock-skills`.
