# Cloud development environment

Use the existing checkout at `/workspace/app`. Each cloud task is already isolated; do not create a Git worktree unless explicitly requested.

The pinned Android SDK, NDK 27.0.12077973, CMake 3.22.1, Gradle cache and API 26/35 emulator images are under `/workspace/toolchains`. JDK 21 comes from the base image. SDK downloads used HTTPS and published artifact verification; the Gradle distribution checksum remains enabled. No signing credentials were added.

```sh
cd /workspace/app
source /workspace/toolchains/activate.sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --no-daemon --max-workers=4
./gradlew assembleRelease bundleRelease --no-daemon --max-workers=4
```

`activate.sh` puts Android/Gradle state in writable directories and derives Java's proxy host/port from the existing `HTTPS_PROXY`, without copying credentials. `setup.sh` recreates the toolchain and activation helper and runs the required debug checks; it has been exercised after installation. Both the complete installer and startup instructions are saved in the cloud environment draft. Saving a draft does not apply network changes, publish the environment, or verify restoration in a fresh task.

## Android checks

The machine has no `/dev/kvm`. Software AVDs are named `nexterm-api35` (ports 5554/5555) and `nexterm-api26` (ports 5556/5557). First boot can take a long time because Android verifies system packages without acceleration. Emulator processes do not survive environment snapshots.

```sh
source /workspace/toolchains/activate.sh
emulator -avd nexterm-api35 -ports 5554,5555 -no-window -no-audio \
  -no-boot-anim -no-snapshot -accel off -gpu swiftshader_indirect \
  -cores 4 -memory 2048 > /tmp/nexterm-emulator.log 2>&1 &
adb -s emulator-5554 shell getprop sys.boot_completed # must return 1
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --no-daemon --max-workers=2
```

Repeat with API 26 and its ports/serial. The SDK's `adb` requires `$HOME/.android`; setup linked an absent path to `/workspace/toolchains/android-user`, preserving existing state. AVD state and emulator registration use writable toolchain directories.

The instrumentation suite has two tests: native PTY Unicode/resize/Ctrl-C/exit, and shell output plus Unicode editor save. Current results must be read from `app/build/outputs/androidTest-results` and `app/build/reports/androidTests`; building the test APK is not a passing Android test run. Physical-device validation remains outstanding. See the README for current validation results.

## Recovery and publication

This checkout started clean at `22e72a6`. Local branches, stashes, reflogs, and unreachable Git objects contained no recoverable terminal improvements. The remote exposed main and existing tags only; no earlier conversation transcript or backup was available in this workspace.

Authenticated Git reads and a dry-run push work through the existing platform Git proxy. The GitHub API request failed at the proxy CONNECT stage with HTTP 403; the running restricted policy excludes `api.github.com`. Draft additions for `api.github.com` and `uploads.github.com` have been saved while preserving the package-manager network presets. Review/save them in environment settings, then publish the environment to activate them. This does not prove API authorization; retry the required API operation afterward before requesting credentials.

The release workflow gates publication on API 26/35 smoke tests, unit tests, lint and debug assembly. Titles and artifacts explicitly identify unfinished development builds. Keystore-signed APK/AAB outputs remain development artifacts. CI execution and GitHub Release publication are not established by a successful local build or Git push.
