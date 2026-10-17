# iOS app — engineering rules

Swift + SwiftUI guidance for `apps/ios/`. OpenCode loads this file when work
touches this directory; the repository rules in the root `AGENTS.md` still apply
and govern anything cross-cutting.

## Conventions

- Swift + SwiftUI by default, UIKit only for a concrete native need; native
  Apple frameworks, and Swift Package Manager where appropriate.
- Apple's Human Interface Guidelines and the Swift API Design Guidelines.
  Respect Dynamic Type, safe areas, semantic colors, system appearance, and
  native navigation.
- Structured concurrency with explicit task lifetimes and `MainActor` for UI
  updates; no force-unwrapping in production paths and no unsafe shared mutable
  state. Details: `docs/ARCHITECTURE.md` §4.
- Source folders are PascalCase (`App/`, `Resources/`; when needed `Core/`,
  `DesignSystem/`). The design system is **per app** — extract `DesignSystem/`
  once a repeated pattern exists, never speculatively
  (`docs/ARCHITECTURE.md` §2.1–§2.2, `docs/DESIGN_STANDARDS.md` §2/§4).
## Build, test, lint

```sh
open apps/ios/BeingMuslim.xcodeproj          # build and run in Xcode

# Or from the CLI — swap `build` for `test` to run the tests:
xcodebuild -project apps/ios/BeingMuslim.xcodeproj \
  -scheme BeingMuslim \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

Toolchain and signing (Xcode version, deployment target, Development Team):
`README.md` §Prerequisites. Formatter tooling (`swift-format`/SwiftLint) is not
set up yet — add it when the app has code worth formatting, and record the
command here.
