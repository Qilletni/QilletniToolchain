# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Centralized own version and upstream dependency pins in `gradle.properties`
  (`toolchainVersion`, `qilletniCoreVersion`, `qilletniPkgutilVersion`,
  `qilletniDocgenVersion`).
- Onboarded the shared release process (`.qilletni/release.yml`, `RELEASE.md`,
  `release-prepare.yml`/`release.yml`/`dependency-update.yml`/`pr-ci.yml`).
- Generated CycloneDX SBOM and `component-manifest.json`, both attached as
  release assets and the manifest embedded in every CLI archive.
- `qilletni --version` now also reports the exact embedded
  `qilletni-core`/`qilletni-api`/`qilletni-pkgutil`/`qilletni-docgen`
  versions and the source commit.
- Gradle dependency locking and a `checkNoSnapshotDependencies` guard.
- `docs/migrations/` convention for major-release migration guides.

### Changed

- Sibling composite-build substitutions (`../Qilletni`, `../QilletniPackageUtility`,
  `../QilletniDocgen`) are now opt-in only via `-PincludeSiblingBuilds=true`;
  stable CI/release builds always force this off.
- Fixed the stale `RubbaBoy/QilletniToolchain` download references in
  `scripts/install.sh` and `deploy/download_toolchain.sh` to `Qilletni/QilletniToolchain`.

## [1.0.1] - 2025-12-01

### Changed

- Updated to Qilletni v1.0.1

## [1.0.0] - 2025-11-01

### Added

- Initial toolchain implementation
