![NexTerm — Your pocket Linux workstation](docs/banner.svg)

[![Build](https://github.com/Cloud77077/app/actions/workflows/build-release.yml/badge.svg)](https://github.com/Cloud77077/app/actions)
![Android](https://img.shields.io/badge/Android-8.0%2B-00b4ff)
![License](https://img.shields.io/badge/license-MIT-blue)
[![Release](https://img.shields.io/github/v/release/Cloud77077/app?include_prereleases)](https://github.com/Cloud77077/app/releases)
[![Stars](https://img.shields.io/github/stars/Cloud77077/app)](https://github.com/Cloud77077/app)

**Development implementation, not a production release.** The requested Termux/VS Code equivalent is not complete. Source is published at Cloud77077/app. Automated signed release publication has not been verified.

## Contents
- [Features and status](#features-and-status)
- [Screenshots](#screenshots)
- [Installation](#installation)
- [Getting started](#getting-started)
- [Terminal reference](#terminal-reference)
- [Editor shortcuts](#editor-shortcuts)
- [SSH guide](#ssh-guide)
- [Building from source](#building-from-source)
- [GitHub Actions setup](#github-actions-setup)
- [Architecture](#architecture)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## Features and status
Implemented code, pending device validation:
- Terminal: JNI PTY creation, interactive Android shell, multiple named sessions, close session, control-key toolbar, UTF-8 output, selectable text.
- Background: foreground service with ongoing notification while sessions exist.
- Files: private workspace, directory navigation, file/folder creation.
- Editor: native Compose text editor, dirty indicator, atomic replacement on save.
- UI: dark gradient, cyan accents, bottom navigation, edge-to-edge insets, screen-awake toggle.
- Build: API 26 minimum, API 35 target, CMake JNI, Gradle wrapper, release-signing configuration.

**Not implemented:** VT screen emulation/ANSI colors/cursor addressing; Linux/PRoot bootstrap; apt/pkg/pip/npm/gem; preinstalled development tools, Claude, gh, code-server or SSH; terminal/editor splits; 10,000-line scrollback guarantee; gesture zoom, bundled fonts, selection handles specific to terminal; TextMate/Tree-sitter, LSP and diagnostics; rename/delete/move/context menus/Git status in explorer; global search; Git operations/diffs/history; extensions or marketplace; palette; JSON settings; minimap, multicursors, folding, bracket matching, formatting; breadcrumbs; SSH manager; cloud sync; snippets; clipboard history; process manager; shared-storage SAF access; hardware editor shortcuts; light theme; blur effects; tablet-specific layouts and animations.

The terminal presently displays raw output, so full-screen applications and escape sequences will not render correctly. Foreground services cannot guarantee survival after Android kills a process or the user force-stops the app. No Linux root filesystem is bundled. Do not represent these missing capabilities as working.

## Screenshots
Capture actual device screenshots after validation. Slots: terminal, editor, Git panel, file explorer. No simulated screenshots are supplied.

## Installation
Once a release exists, download its APK from Releases and permit your browser/file manager to install unknown apps. Install, launch NexTerm and select **Open terminal**. First launch creates a private workspace; it does not download Linux packages. Current builds are for development only.

## Getting started
Open a terminal, type `pwd` and tap Send. `ls`, `echo hello`, and Android toybox commands use the Android system shell. Use Files to create a file, tap it to open the editor, and Save to persist changes.

Package installation, `claude`, repository cloning and Git-panel commit/push workflows require the missing Linux bootstrap. No working instructions for those features can be supplied yet. When implemented, Claude will also require the user's own authentication with Anthropic.

## Terminal reference
Available commands are those supplied by the device's `/system/bin`, not a preinstalled Linux development toolchain. `git`, `python3`, `node`, `gh`, `claude`, `apt` and `pkg` are **not** supplied by this project. Toolbar: Esc, Tab, Ctrl-C, arrows, pipe, slash, dash, tilde, braces and semicolon.

## Editor shortcuts
| Action | Current interface |
|---|---|
| Save | Save button |
| Open | Tap file in Files |
| Copy/paste | Android text field selection |
| Ctrl+S / Ctrl+Z / command palette | Not implemented |

## SSH guide
SSH client/server, key storage and forwarding are not yet implemented. Do not store private keys in repository files. Future integration needs host-key verification, secure key handling and explicit server startup controls.

## Building from source
Prerequisites: JDK 17 or 21, Android SDK platform 35/build-tools 35.0.0, NDK 27.0.12077973, CMake 3.22.1. Open this directory in Android Studio and let Gradle sync, or run:

```sh
git clone https://github.com/Cloud77077/app.git
cd app
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'ndk;27.0.12077973' 'cmake;3.22.1'
./gradlew assembleDebug
./gradlew assembleRelease bundleRelease
```

Set `ANDROID_HOME` or `sdk.dir` in an ignored `local.properties`. Release builds are unsigned unless all signing variables are supplied: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Never commit the keystore or credentials. Install a debug APK using `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Validate on API 26 and 35 devices before distributing.

## GitHub Actions setup
Configure repository Actions secrets `SIGNING_KEY` (base64 encoded keystore), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Keep the keystore backed up securely: future updates require the same signing identity. The workflow builds and signs both APK and AAB with Gradle, verifies their signatures, and publishes prereleases on main pushes or semantic version tags. Main pushes use `1.0.<run_number>`; explicit tags must be `vMAJOR.MINOR.PATCH`. Version codes use the monotonically increasing workflow run number; reruns reuse that code. Avoid reusing release tags. PRs run validation without access to signing secrets. Releases stay prereleases until product readiness is established.

## Architecture
`Pty.kt` bridges to `pty.cpp`, which opens a pseudo-terminal, forks, establishes a controlling terminal and executes the Android shell. `Sessions` owns session state and asynchronous I/O; `TerminalService` keeps the process eligible for foreground execution. Compose hosts terminal output, navigation, file browsing and the native text editor. PTY handles live only in memory. LSP and Linux runtime modules are not present yet.

A production Linux bootstrap must use reviewed, hash-verified ABI-specific artifacts and respect Android executable-memory and app-data execution restrictions. It must map workspace paths consistently and recover safely from interrupted installations. Adding a download URL alone is insufficient. Reusing Termux components requires a separate license review; this project's MIT license does not relicense third-party code.

## Contributing
Open an issue describing expected behavior and device/API details. Keep credentials out of logs. Test PTY resize, Unicode, full-screen applications, lifecycle transitions, process reaping and storage errors. Add tests covering the actual implemented behavior. Track missing features above; don't replace them with nonfunctional controls.

## License
MIT for original source; see [LICENSE](LICENSE). Dependencies retain their respective licenses.

## Acknowledgements
Termux, libterm, JediTerm, code-server and Tree-sitter informed the requested design. They are not bundled or integrated in this implementation. Android, Kotlin, Jetpack Compose and Gradle provide the current build and UI foundations.
