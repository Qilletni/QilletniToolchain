# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added automatic release tooling with a generated SBOM
- Includes detailed versions in `qilletni --version`
- `docs/migrations/` convention for major-release migration guides.

### Changed

- Sibling composite-build substitutions (`../Qilletni`, `../QilletniPackageUtility`,
  `../QilletniDocgen`) are now opt-in only via `-PincludeSiblingBuilds=true`;
  stable CI/release builds always force this off.

## [1.0.1] - 2025-12-01

### Changed

- Updated to Qilletni v1.0.1

## [1.0.0] - 2025-11-01

### Added

- Initial toolchain implementation
