---
name: make-pr
description: Use when creating or revising a pull request — its body, and which issues it closes on merge (the closing "magic words"). Composes the `pr` skill's template with GitHub's closing-keyword rules.
---

# Make a PR

Own the pull request: its body, and **which issues it closes on merge**. The body's *shape* is not yours to invent — call the `pr` skill for its template. This skill adds what `pr` does not cover: GitHub's **closing keywords**, the "magic words" that turn `Closes #12` in the description into a closed issue when the pull request merges.

GitHub's rules are stated once, here. They are stable and live in exactly one place — this file. If a keyword that looks right still does not link (step 4), read the source before trusting this copy:

<https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue>

## 1. Gate on the base branch

Keywords are interpreted **only when the pull request targets the repository's default branch**. Any other base makes every keyword inert: no links are created and merging closes nothing.

```sh
PR=24                              # the pull request in question
DEFAULT=$(gh repo view --json defaultBranchRef --jq .defaultBranchRef.name)
printf '%s %s\n' "$DEFAULT" "$(gh pr view "$PR" --json baseRefName --jq .baseRefName)"
```

When they differ, retarget — `gh pr edit "$PR" --base "$DEFAULT"` — or, for a pull request that does not exist yet, just open it against the default branch (`gh pr create --base "$DEFAULT"`). If neither is possible, say so in the body instead of promising a close, and get the issues closed another way — link them manually from the pull request's *Development* sidebar, or close them by hand after merge.

## 2. Compose the body

Call the `pr` skill with the `skill` tool and follow its template — Summary, Evidence, Merge Danger. Then close it with the line from step 3.

## 3. Give every issue the full keyword

GitHub's syntax table requires the full form **per issue** — a bare `#3, #4` after one keyword does not link the rest:

```text
Closes #2, Closes #3, Closes #4
Fixes octo-org/octo-repo#100
```

- Same repository: `KEYWORD #NUMBER`. Another repository: `KEYWORD OWNER/REPO#NUMBER` — that closes the issue over there, where step 4's check cannot see it.
- Any of `close`, `closes`, `closed`, `fix`, `fixes`, `fixed`, `resolve`, `resolves`, `resolved`. Colons and uppercase are accepted (`Closes: #10`, `CLOSES #10`).
- A closing keyword in a commit message works too, but then the pull request is not listed as linked.

## 4. Verify — the finish line

Done only when GitHub reports every issue the body claims to close:

```sh
OWNER=humayunkabir REPO=being-muslim PR=24
gh api graphql -f query='{repository(owner:"'"$OWNER"'",name:"'"$REPO"'"){pullRequest(number:'"$PR"'){closingIssuesReferences(first:100){totalCount nodes{number}}}}}' \
  --jq '.data.repository.pullRequest.closingIssuesReferences | "\(.totalCount) \(.nodes|map(.number))"'
```

The check is `totalCount` against the number of issues the body claims — not the length of `nodes`, which `first` caps and which would silently hide a 26th keyword. `closingIssuesReferences` covers **this** repository only, so a cross-repository keyword (step 3) closes its issue but never appears here; verify those on the other repository's issue instead.

A short count means the gate in step 1 failed, or the rules above have drifted from the source — check it. GitHub can also lag; re-run before concluding. Links are derived from the description as it stands, so **re-verify after every body edit**; removing the keyword itself is the only way to unlink a keyword-linked issue.

## This repository

Tracker conventions and the `gh` invocation live in `docs/agents/issue-tracker.md`.
