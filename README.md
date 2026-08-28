<div align="center">

  <img src="https://github.com/tapframe/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="Nuvio X" width="300" />
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

Nuvio X is the current Kotlin Multiplatform rewrite of the original React Native app. It delivers a Compose UI for Android while keeping the playback-focused experience, collection tools, watch progress flows, downloads, and Stremio addon ecosystem integration that shaped the earlier app.

Nuvio X is built on Nuvio by NuvioMedia.

The mobile app is built from a single codebase in [composeApp](./composeApp), with a native Android entry point.

## Installation

### Android

Download the latest Android build from [GitHub Releases](https://github.com/NuvioMedia/NuvioMobile/releases/latest).

## Development

```bash
git clone https://github.com/NuvioMedia/NuvioMobile.git
cd NuvioMobile
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

For comprehensive legal information, including our full disclaimer, third-party extension policy, and DMCA/Copyright information, please visit our [Legal & Disclaimer Page](https://nuvioapp.space/legal).

## Built With

- Kotlin Multiplatform
- Compose Multiplatform
- Kotlin
- AndroidX Media3

## Star History

<a href="https://www.star-history.com/#NuvioMedia/NuvioMobile&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=NuvioMedia/NuvioMobile&type=date&legend=top-left" />
 </picture>
</a>

<!-- MARKDOWN LINKS & IMAGES -->
[contributors-shield]: https://img.shields.io/github/contributors/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[contributors-url]: https://github.com/NuvioMedia/NuvioMobile/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[forks-url]: https://github.com/NuvioMedia/NuvioMobile/network/members
[stars-shield]: https://img.shields.io/github/stars/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[stars-url]: https://github.com/NuvioMedia/NuvioMobile/stargazers
[issues-shield]: https://img.shields.io/github/issues/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[issues-url]: https://github.com/NuvioMedia/NuvioMobile/issues
[license-shield]: https://img.shields.io/github/license/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[license-url]: https://github.com/NuvioMedia/NuvioMobile/blob/main/LICENSE
