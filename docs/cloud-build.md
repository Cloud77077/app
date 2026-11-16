# Current cloud workspace

Use the existing checkout at `/workspace/app`; this task is already isolated, so no Git worktree is needed.

SDK, CMake and NDK are installed at `/workspace/toolchains/android-sdk`. Gradle artifacts are cached under `/workspace/toolchains/gradle`. The ignored `local.properties` points to this SDK.

In this managed environment, Java requires the configured HTTP proxy explicitly and Android state must use a writable location:

```sh
cd /workspace/app
export ANDROID_USER_HOME=/workspace/toolchains/android-user
export GRADLE_USER_HOME=/workspace/toolchains/gradle
export JAVA_TOOL_OPTIONS='-Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080'
./gradlew assembleDebug assembleRelease bundleRelease testDebugUnitTest lintDebug --no-daemon --max-workers=4
```

These proxy names are specific to this environment. On other machines, use their normal Android Studio/SDK setup. No signing credentials are stored here. No emulator or Android device has been connected, and no application behavior has been validated on a device. The GitHub API returned `Forbidden`, but authenticated Git push succeeded. Source is published on the main branch of Cloud77077/app. Release workflow execution has not been verified.
