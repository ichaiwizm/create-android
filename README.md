# Create — Android

Native Jetpack Compose client for **Create**, a mobile-first AI image and video generation studio
built on the kie.ai model APIs. Write or dictate a prompt, attach reference images, pick a model,
generate, then browse, share and export the results.

Android sibling of [create-ios](https://github.com/ichaiwizm/create-ios): same product, same backend
(Next.js API routes over PocketBase), same frozen HTTP contract (`CONTRACTS.md`).

## Architecture

- **Two screens** — *Create*, a chat-like composer (prompt, voice dictation, reference thumbnails,
  model and settings bottom sheets) above an inverted feed of recent generations; and *Gallery*, a
  grid with a full-screen lightbox offering share, one-click upscale and cutout, save and delete.
- **Asynchronous generation flow** — generations are created server-side and reconciled by polling
  plus a Firebase Cloud Messaging notification when the media is ready and repatriated.
- **Data-driven model catalogue** (`data/catalog/`) — models and their parameters are data, so the
  composer builds its controls from the catalogue instead of hardcoding a screen per model.
- **Networking** (`core/net/`) — Retrofit and OkHttp with Moshi, an auth interceptor and a token
  authenticator that transparently refreshes the PocketBase session; tokens are kept in encrypted
  storage (`androidx.security.crypto`).
- **Manual dependency injection** — a plain `AppContainer` graph rather than a DI framework.
- **Media** — Coil for images, Media3 ExoPlayer for generated video, MediaStore saving and
  FileProvider sharing.
- **Design** — Material 3 Expressive with a custom iridescent accent, shimmer placeholders, shared
  element transitions between the gallery and the lightbox, haptics.
- **Release pipeline** — a GitHub Actions workflow builds and uploads to a Play track, with the
  keystore and Play service account injected as secrets.

## Stack

Kotlin, Jetpack Compose (Material 3), Navigation Compose, Retrofit + OkHttp + Moshi, DataStore,
Coil, Media3, WorkManager, Firebase Cloud Messaging, Gradle version catalogs, GitHub Actions.

## Build

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Requires JDK 17. Backend base URLs are injected as `buildConfigField` values, so they can be pointed
at a test backend. The app requires an account on the hosted backend and public signup is closed, so
a clean clone builds and runs but cannot log in.

## Status

Not published on the Play Store.

## Licence

No licence granted. Published for reading.
