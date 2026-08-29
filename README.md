<div align="center">

  <img src="composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark.png" alt="Nuvio X" width="300" />
  <br />
  <br />

  [![Contributors][contributors-shield]][contributors-url]
  [![Forks][forks-shield]][forks-url]
  [![Stargazers][stars-shield]][stars-url]
  [![Issues][issues-shield]][issues-url]
  [![License][license-shield]][license-url]

  <p>
    A modern media hub for Android built with Kotlin Multiplatform and Compose Multiplatform.
    <br />
    Stremio addon ecosystem
  </p>

</div>

## About

Nuvio X is a media hub for Android built with Kotlin Multiplatform and Compose Multiplatform. It compiles a shared codebase into a single native Android app, delivering a Compose UI with playback, collections, watch-progress tracking, downloads, and full Stremio addon ecosystem support.

Nuvio X is a feature fork of **Tuvora Mobile**, itself derived from the original **Nuvio** app. It is distributed under the GPLv3 license.

## Installation

### Android

Download the latest Android build from [GitHub Releases](https://github.com/mezbahzaman/TuvoraMobileX/releases/latest).

## Changes from Tuvora Mobile

Nuvio X is a focused evolution of Tuvora Mobile rather than a rewrite. In plain terms:

**Rebranded**
- Renamed the app from *Tuvora* to *Nuvio X*, including new branding throughout the interface.

**Changed**
- Switched to a new application logo and splash presentation.
- Moved versioning to a single source of truth (`version.properties`).
- Hardened the player, download, and security subsystems.
- Added cleartext HTTP support so public IPTV panels can connect.
- Fixed hero catalogue selections so they persist and are no longer auto-overridden.
- Made built-in (embedded) subtitles load automatically at playback start.

**Added**
- Android-only focus: a leaner, single-platform build pipeline.

**Removed**
- Full iOS support (the app is now Android-only).
- The MPVKit iOS library and its related code.
- The sports / radar feature.
- Various orphaned source sets and unused iOS catalog entries.

## Credits

Nuvio X builds on the work of others and we are glad to acknowledge it.

- **[Nuvio](https://github.com/NuvioMedia)** — original author of the codebase this project is built on.
- **[Paradox-Kush](https://github.com/paradox-kush)** — developer of Tuvora Mobile, the version of the app this project forked from.

Nuvio X is provided under the terms of the [GPLv3 license](LICENSE).

## Development

```bash
git clone https://github.com/mezbahzaman/TuvoraMobileX.git
cd TuvoraMobileX
./scripts/run-mobile.sh android
```

### Project Structure

- `composeApp/` contains the shared Kotlin Multiplatform and Compose Multiplatform app code.
- `composeApp/src/commonMain/` contains shared UI, features, repositories, and platform-agnostic logic.
- `composeApp/src/androidMain/` contains Android-specific integrations.

Useful commands:

```bash
./gradlew :composeApp:assembleDebug
./scripts/build-distribution.sh
```

Versioning is driven from `version.properties`, which is the source of truth for version numbers.

## Legal & DMCA

Nuvio X functions solely as a client-side interface for browsing metadata and playing media provided by user-installed extensions and/or user-provided sources. It is intended for content the user owns or is otherwise authorized to access.

Nuvio X is not affiliated with any third-party extensions, catalogs, sources, or content providers. It does not host, store, or distribute any media content.

## Built With

- Kotlin Multiplatform
- Compose Multiplatform
- Kotlin
- AndroidX Media3

<!-- MARKDOWN LINKS & IMAGES -->
[contributors-shield]: https://img.shields.io/github/contributors/mezbahzaman/TuvoraMobileX.svg?style=for-the-badge
[contributors-url]: https://github.com/mezbahzaman/TuvoraMobileX/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/mezbahzaman/TuvoraMobileX.svg?style=for-the-badge
[forks-url]: https://github.com/mezbahzaman/TuvoraMobileX/network/members
[stars-shield]: https://img.shields.io/github/stars/mezbahzaman/TuvoraMobileX.svg?style=for-the-badge
[stars-url]: https://github.com/mezbahzaman/TuvoraMobileX/stargazers
[issues-shield]: https://img.shields.io/github/issues/mezbahzaman/TuvoraMobileX.svg?style=for-the-badge
[issues-url]: https://github.com/mezbahzaman/TuvoraMobileX/issues
[license-shield]: https://img.shields.io/github/license/mezbahzaman/TuvoraMobileX.svg?style=for-the-badge
[license-url]: https://github.com/mezbahzaman/TuvoraMobileX/blob/main/LICENSE
