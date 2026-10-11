# Greeting Sorter

An Android app that finds the Good Morning, Good Night, festival, birthday and other
greeting pictures you receive on WhatsApp and Telegram, and sorts them into groups so you
can view, share or delete a whole group at once.

The text on each picture is read on the phone with Google ML Kit (English and Hindi).
Nothing is uploaded anywhere.

## Groups

Good Morning · Good Afternoon · Good Evening · Good Night · Birthday · Anniversary ·
Festivals · Other Wishes · Not a greeting

If a picture lands in the wrong group, open it and use the pencil button to move it.
The app remembers your choice.

## Getting the APK

**From GitHub (no tools needed):** push this folder to a GitHub repository. The
`Build APK` workflow builds the app on every push to `main` and attaches
`GreetingSorter.apk` to a release called `latest`. Open that release on your phone,
download the APK and tap it to install (allow "Install unknown apps" when asked).

**With Android Studio:** open this folder, wait for Gradle to sync, then
*Build › Build App Bundle(s) / APK(s) › Build APK(s)*, or press Run with a phone connected.

**From the command line:** with the Android SDK installed and `ANDROID_HOME` set,
run `./gradlew assembleRelease`. The APK is at `app/build/outputs/apk/release/app-release.apk`.

The release build is signed with the debug key so it installs without setup. Use your own
signing key before publishing to the Play Store.

## How it works

| File | What it does |
|---|---|
| `ImageRepository.kt` | Lists images from the phone's media library, filtered to WhatsApp, Telegram, Downloads or other folders, and caches results so re-scans only read new images. WhatsApp's `Sent` folder is skipped. |
| `TextReader.kt` | Shrinks each image and reads its text with ML Kit (Latin first, Devanagari if nothing matched). |
| `GreetingClassifier.kt` | Matches the text against greeting phrases in English and Hindi. Add phrases here to improve sorting. |
| `MainViewModel.kt` | Runs the scan, keeps the groups, handles moving and deleting. |
| `ui/` | The screens: home with group list, group grid with multi-select, full-screen viewer. |

Deleting uses Android's own confirmation prompt on Android 11 and newer, and the app's
own prompt on older phones. Supports Android 8.0 and newer.

Run the matching tests with `./gradlew testDebugUnitTest`.
