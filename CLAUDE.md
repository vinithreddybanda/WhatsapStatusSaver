# WhatsApStatusSaver development notes

## Android implementation rules

- Keep the existing Kotlin, Jetpack Compose, Material 3, ViewModel and repository stack. Do not add dependencies unless they solve a concrete app problem.
- Keep directory enumeration, image metadata reads, copies and deletes off the main thread.
- Show gallery content progressively. Do not block the screen on a full-directory scan or launch duplicate scans from recomposition and lifecycle callbacks.
- Use lazy staggered grids with stable item keys and content types. Preserve intrinsic image ratios; do not force every media card to the same height.
- Load downsampled thumbnails through Coil. Cache by file path plus modification time, avoid expensive video-frame work during fast scrolling, and do not crossfade every thumbnail.
- Keep modal blur scoped to the background while a sheet is visible. Avoid permanent blur layers during normal scrolling.
- Use WindowInsetsControllerCompat for immersive status-bar behavior. Respect display cutouts, navigation gestures and system-owned UI.
- Keep storage access user initiated. Explain why broad access is required before opening Android's special-access screen.
- Use case-insensitive file extensions and grant read URI permissions when opening or sharing files.
- Keep UI state in the ViewModel and filesystem operations in the repository.

## Validation

Before merging:
- Run `./gradlew clean assembleGithubDebug assembleFdroidRelease`.
- Run unit tests with `./gradlew test`.
- On a physical device, test first-run permission flow, returning from Settings, rapid scrolling through mixed image/video statuses, saving, deleting, sharing, opening with another app, and navigation-bar/cutout layouts.
- Capture a screen recording and inspect logcat when reproducing frame drops.

## Optional Claude Code tooling

The repository includes `.mcp.json` for the open-source `mcp-mobile-interaction` server. It requires Node.js, Android SDK platform tools, ADB and a connected Android device or emulator.

For Google's maintained Android agent guidance, add the official marketplace in Claude Code and install its Android skills plugin:

```text
/plugin marketplace add android/skills
/plugin install android-skills@android-skills
```

MCP and agent skills are development tools only. They are not Android runtime dependencies.
