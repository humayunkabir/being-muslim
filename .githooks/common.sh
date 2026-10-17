#!/bin/sh
# Shared helpers for the versioned git hooks in .githooks/.
#
# Activate once per clone:  git config core.hooksPath .githooks

# Return 0 when the path matches a credential/keystore pattern.
# Mirrors the "Secrets" section of the root .gitignore.
is_sensitive_path() {
    base=${1##*/}
    base=$(printf '%s' "$base" | tr '[:upper:]' '[:lower:]')
    # The committed env template is always allowed.
    [ "$base" = ".env.example" ] && return 1
    case "$base" in
        *.p12|*.pfx|*.p8|*.pem|*.jks|*.keystore|*.mobileprovision)
            return 0 ;;
        .env|.env.*)
            return 0 ;;
    esac
    return 1
}

# Return 0 when the path is excluded by .gitignore rules
# (catches files staged with `git add -f`).
# --no-index: without it, check-ignore skips paths already in the index,
# which would make this check useless for staged files.
is_ignored_path() {
    git check-ignore -q --no-index -- "$1"
}

# Return 0 when stdin (file content) looks like it contains a credential.
# Patterns are written so they do not match their own source text.
content_has_secret() {
    grep -I -q -E \
        -e '-----BEGIN [A-Z0-9 ]*PRIVATE KEY' \
        -e 'ghp_[A-Za-z0-9]{36}' \
        -e 'github_pat_[A-Za-z0-9_]{22,}' \
        -e 'AKIA[0-9A-Z]{16}' \
        -e 'ASIA[0-9A-Z]{16}' \
        -e 'AIza[0-9A-Za-z_-]{35}' \
        -e 'xox[baprs]-[A-Za-z0-9-]{10,}' \
        -e 'sk-[A-Za-z0-9_-]{40,}' \
        2>/dev/null
}
