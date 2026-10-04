# Kaggle Controller

*Your Kaggle workspace, redesigned for Android.*

Kotlin + Jetpack Compose + Material 3. No backend server: the phone talks straight to Kaggle with your own API token.

> **Status: v0.1, a first working foundation, not the full 66-section spec.** It was written without access to an
> Android SDK, so **it has not been compiled yet**. Expect to fix a few small compile errors on first build (see
> "First build" below). Read `docs/API_RESEARCH.md` to see what Kaggle's API really allows.

## What works in v0.1

| Area | Status |
|---|---|
| Sign in with a Kaggle API token (verified, then stored encrypted with Android Keystore) | Built |
| Notebook list: search, mine/public, favorites, drafts, running, failed, pagination, pull-to-refresh | Built |
| New / import (.ipynb .py .r .rmd) / pull from Kaggle / delete (with confirmation) | Built |
| Editor: cells, scripts, syntax highlighting, line numbers, undo/redo, find/replace, auto-indent, phone key toolbar, font and tab size | Built |
| Local drafts: autosave, offline editing, labels (Saved locally / Syncing / Synced / Unsaved / Sync failed) | Built |
| Save to Kaggle (quick save) and Save + Run (with confirmation dialog) | Built |
| Run monitor: status, log snapshot, search, jump to error, auto-scroll, copy/share, backoff polling | Built |
| Output files: list + download through Android DownloadManager | Built |
| Run-finished notifications (best effort via WorkManager) | Built |
| Explore: datasets, models, competitions (search, infinite scroll, Open in Kaggle) | Built |
| Capability map + "Open in Kaggle" fallbacks | Built |
| Themes: system / light / dark / AMOLED, dynamic colour | Built |
| Phone bottom navigation and tablet navigation rail; kaggle.com/code links open in the app | Built |
| Datasets/models create+upload, competition submit, forums, benchmarks, workflows, AI assistant, diff viewer, resource monitor, command palette | **Not built yet** (see roadmap) |
| Cancel run, server schedules, secrets | **Not available in Kaggle's public API** -> Open in Kaggle |

## Requirements

- Android Studio Ladybug (2024.2) or newer, JDK 17
- Android 8.0+ (minSdk 26) device or emulator
- A Kaggle account

## Setup

1. Open the `KaggleController` folder in Android Studio. Let it sync (it will download Gradle 8.9 and the
   libraries listed in `gradle/libs.versions.toml`).
2. Run the `app` configuration on a device or emulator.
3. On Kaggle: **Settings -> API -> Generate New Token**. Copy the `KGAT_...` token.
4. Paste it into the app. It is checked with Kaggle first, then stored encrypted on the phone.

Command line (after Android Studio created the Gradle wrapper, or with a local Gradle 8.9):

    gradle :app:assembleDebug
    gradle :app:testDebugUnitTest

Unit tests use a mock web server. No real credentials are needed.

## First build: likely fixes

Because it was never compiled, a few things may need a nudge:

- **Version numbers** in `gradle/libs.versions.toml` are a known-good baseline. Accept Android Studio's update hints.
- If a Material icon name does not exist in your version, swap it for a similar one in the red-underlined line.
- If a list shows blank titles, Kaggle's response field names differ from my guesses. Edit the key lists in
  `data/kaggle/KaggleRepositories.kt`. Nothing else needs to change.
- If a call returns 404/405, correct the method name in `Rpc` / the repository. All calls are in that one file.

## Security notes

- The token is encrypted with a non-exportable AES-256-GCM key in the Android Keystore, excluded from backups.
- The token is only sent to `https://api.kaggle.com` (and never to signed download URLs).
- It is never logged and never put in error messages.
- No analytics, no telemetry, no custom server.
- Use a token you can revoke: Kaggle Settings -> API. Create a separate token for the app.

## Required scopes

Personal API tokens act as your account, so there are no per-scope choices today. Revoke the token to remove access.

## Known Kaggle API limitations (and how the app handles them)

- **Run = save.** Kaggle has no separate run call. RUN saves a new version and runs it. Each run creates a version.
- **Logs are snapshots**, not a live stream. The monitor polls (4s growing to 60s) and stops when the run ends.
- **No cancel, no server schedules, no secrets, no run-history list** in the public API -> shown as "Open in Kaggle".
- **Run history** in the app only contains runs started from this app.
- **Background notifications** use WorkManager. Android may delay them (Doze, battery saver). They do not control the run.

## Website-only fallback

Every unsupported action shows **Open in Kaggle**, opened in a Chrome Custom Tab. The registry is
`core/common/Capabilities.kt`. The More tab shows it live.

## Project layout

    app/src/main/java/com/kagglecontroller/
      core/        network (RPC client), security (Keystore), common (ApiState, Capabilities), ui (theme, components)
      data/        kaggle (repositories), local (drafts, runs, settings)
      domain/      models, repository interfaces, ipynb document model
      feature/     auth, home, notebooks, editor, runs, explore, schedules, files, more

## Roadmap (suggested order)

1. First compile + live smoke test with your token
2. Dataset create/version/upload (signed-URL flow), competition submit with confirmation
3. Version history + diff viewer (via `versionLabel`)
4. Local (Android) schedules and the workflow builder
5. Optional AI assistant (bring your own key, never on by default)
6. Command palette, resource monitor via diagnostic cells, tablet 3-pane layout
