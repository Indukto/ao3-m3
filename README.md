# Archive

**An unofficial, offline-first AO3 reader for Android.**  
Kotlin · Jetpack Compose · Material 3 Expressive

![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)

Archive is a reading-focused client for the [Archive of Our Own](https://archiveofourown.org/).
It is built for finding something good, reading it comfortably, and keeping it available when
AO3 is slow or you are offline.

> Archive is an unofficial fan project and is not affiliated with or endorsed by the Organization
> for Transformative Works (OTW) or AO3.

## Features

- **Discover** — pick up where you left off, browse popular works, and see updates from your
  subscriptions. Open a work directly from a pasted AO3 link or work ID, and refresh without
  leaving the screen.
- **Search** — free-text search with filters for rating, archive warnings, category, fandom,
  relationship, character, tags, word count, completion, one-shots, and crossovers. Use AO3's
  sort orders, page through results, and save searches for later.
- **Work details** — title, byline, stats, summary, notes, collapsible tags, chapter list, and a
  split read/continue action. Bookmark, mark for later, or subscribe from the same screen.
- **Reader** — serif or sans type, adjustable text size, line height, margins, and optional
  justification. Choose system, light, sepia, or dark paper, hide the controls while reading,
  move between chapters, and resume at the paragraph where you stopped.
- **Library** — bookmarks, marked-for-later, subscriptions, and history. Cached works stay
  readable offline; history is kept locally on the device.
- **Settings** — dynamic wallpaper colour on Android 12+, light/dark/system brightness, reading
  defaults, account, cache controls, and the AO3 disclaimer.
- **Offline-first behaviour** — every screen prefers cached content. During AO3 maintenance the
  app keeps cached works available and shows a quiet banner instead of a blocking dialog.

## Requirements

- **Android 8.0 (API 26)** or newer.
- Android 12 (API 31) or newer for dynamic wallpaper colour. Older versions fall back to the
  built-in palette.
- To build: **JDK 17+** and Android SDK **Platform 37** with **Build Tools 37**.
- The app targets and compiles against API 37 because the current AndroidX/Compose dependencies
  require it.

## Build and run

Open the project in Android Studio, let Gradle sync, and run the `app` configuration on a device or
emulator. From the command line:

```bash
# Debug APK
./gradlew :app:assembleDebug

# Install it on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Useful checks:

```bash
./gradlew test
./gradlew :app:lintDebug
```

An unsigned release build can be produced with:

```bash
./gradlew :app:assembleRelease
```

No signing configuration is committed, so the release APK is unsigned by default. There is no
prebuilt APK or store listing.

## Architecture

| Module | Responsibility |
| --- | --- |
| `:app` | Single activity, navigation shell, Hilt setup, onboarding |
| `:core:model` | Pure domain models for works, chapters, library entries, searches, and settings |
| `:core:designsystem` | Theme, colour/type/shape/spacing tokens, shared Compose components |
| `:core:data` | AO3 client and HTML parsers, Room cache, repositories, preferences |
| `:feature:discover` | Discover feed and Search |
| `:feature:work` | Work details and the Reader |
| `:feature:library` | Bookmarks, marked for later, subscriptions, and history |
| `:feature:settings` | Settings and the WebView login flow |

Reads are cache-first: Room renders immediately, the network revalidates, and a failed request
never deletes saved content. Browse and search pages are cached too, so the paging source can fall
back to a saved page when AO3 is unreachable. All AO3 URLs live in `Ao3Urls.kt`, and all CSS
selectors live in `Ao3Selectors.kt`, keeping scraper maintenance contained.

The UI is a single-activity Compose app with MVVM feature screens, Hilt dependency injection,
Room for persistence, Paging 3 for feeds, and DataStore for reader and appearance preferences.

## Talking to AO3

AO3 does not provide a public API, so the app reads the same HTML pages a browser would. Requests
are deliberately conservative:

- one request at a time, with at least **two seconds** between requests;
- no background crawling or parallel fan-out;
- an honest `User-Agent` that identifies the app;
- capped exponential backoff for 429/5xx responses and I/O errors;
- AO3's "temporarily denied" page and HTTP 418 are treated as maintenance, never retried in a
  loop.

Library actions re-post AO3's own forms with the current authenticity token instead of hard-coding
endpoint fields. This keeps bookmark, marked-for-later, and subscription changes aligned with the
site.

Scraping is inherently fragile: if AO3 changes its markup, a parser may need updating. Cached
works remain readable while that happens.

## Privacy

- No analytics, crash reporting, advertising, or server-side component.
- Login happens in AO3's own login page inside a WebView. Your password goes directly to AO3 and
  never passes through app code.
- Only AO3's session cookie is stored, encrypted with a non-exportable Android Keystore AES-GCM
  key before it reaches disk.
- `allowBackup` is disabled, and the backup rules exclude the session store and cache.
- Reading history is local to the device and does not require an account.
- Cached chapters can be cleared from Settings at any time.

## Project status

Archive is an early personal project at **0.1.0**. The core reading flow is functional, but expect
rough edges and breaking changes while it develops.

Known limitations:

- AO3's HTML and form fields can change without notice; the scraper and search filters may need
  maintenance.
- Search and library updates require a connection to AO3. Cached content remains available when
  the archive is down.
- The app has been developed and tested primarily on a Pixel 7 Pro (API 37). Tablet and large-screen
  layouts have not been broadly tested.
- Dynamic colour requires Android 12+; older devices use the static palette.
- Release builds are unsigned, and no prebuilt APK is distributed.

## Disclaimer

Archive is an unofficial, fan-made client. It is not affiliated with, endorsed by, or supported by
the Organization for Transformative Works or the Archive of Our Own. AO3 and its works belong to
their respective creators. If you enjoy a work, leave kudos or a comment on AO3.

## License

No license has been added to this repository yet.
