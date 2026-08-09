## QilletniToolchain Release Protocol

This repository is one of the five onboarded release producers in the wider Qilletni
ecosystem (**Qilletni**, **QilletniToolchain**, **QPMCLI**, **QilletniPackageUtility**,
**QilletniDocgen**). It declares its own `.qilletni/release.yml`
(schema/validation in `Qilletni/Qilletni`'s `tools/release/src/release_config.ts`).
The central, reusable release-preparation/dependency-update logic lives in
`Qilletni/Qilletni` (`.github/workflows/reusable-*.yml`); this repository's own
`.github/workflows/` files are thin local callers of it. See `Qilletni/Qilletni`'s own
`RELEASE.md` and `tools/release/README.md` for the full cross-repository contract.

Unlike `Qilletni`, `QPMCLI` or `QilletniPackageUtility`/`QilletniDocgen`, this CLI is not
published to Maven Central. It publishes a single executable archive (and the shadowed
jar) as a GitHub Release asset, registered as `qilletni-toolchain` (`kind: cli`) in
`.qilletni/release.yml`.

### Version centralization

This CLI's own version (`toolchainVersion`) and the exact pinned versions of every
upstream Maven dependency it embeds are all declared once in `gradle.properties`:

- `toolchainVersion` - this repository's own release unit.
- `qilletniCoreVersion` - aligns `dev.qilletni.impl:qilletni` (core) and
  `dev.qilletni.api:qilletni-api` (API) as **one** upstream release unit (they are always
  released together upstream, at the same version, as `qilletni-core`), so a single
  property bumps both coordinates atomically.
- `qilletniPkgutilVersion` - `dev.qilletni.pkgutil:qilletni-pkgutil`.
- `qilletniDocgenVersion` - `dev.qilletni.docgen:qilletni-docgen`.

`includeSiblingBuilds` (default `false`) and `useMavenLocal` (default `false`) are both
opt-in only, for local multi-repo development against sibling checkouts (`../Qilletni`,
`../QilletniPackageUtility`, `../QilletniDocgen`) or a locally-published artifact. Every
CI/release workflow in this repository always passes `-PincludeSiblingBuilds=false`
explicitly, regardless of the default.

### Preparing a release

1. Run the `Release - Prepare` workflow (`workflow_dispatch`), choosing a
   `patch`/`minor`/`major` bump.
2. It calls `Qilletni/Qilletni/.github/workflows/reusable-release-prepare.yml@master`,
   which computes the next version from the latest stable `vX.Y.Z` tag, requires a
   non-empty `## [Unreleased]` section in `CHANGELOG.md` (and, for a `major` bump, a
   `docs/migrations/X.Y.Z.md` guide - see `docs/migrations/README.md`), runs
   `./gradlew clean test`, updates `toolchainVersion`, promotes the changelog, writes a
   `release/pending-release.json` marker, and opens a signed, review-only PR.
3. **This PR never auto-merges.**

### Publishing a release (fully automatic after merge)

`release.yml` reacts to `master` pushes and to `vX.Y.Z` tag pushes:

- **`tag-release`** (every push to `master`): if `toolchainVersion` is still a
  `-SNAPSHOT`, nothing happens. Otherwise, `release/pending-release.json` is required and
  validated, the merge commit's originating PR is checked (`check-merge-provenance`), and
  the immutable `vX.Y.Z` tag is **idempotently** created at that exact commit - no
  maintainer ever has to push a tag by hand. A direct tag push remains supported only as a
  manual recovery path, and is likewise idempotent.
- **`publish-snapshot`** (every push to `master`, isolated from the tag-triggered path):
  publishes the executable archive as the floating `snapshot` GitHub prerelease whenever
  the version is still a `-SNAPSHOT`.
- **`build-and-publish`** (triggered only by the `vX.Y.Z` tag push, inside the protected
  `production-release` GitHub Environment):
  1. Validates the tag matches `toolchainVersion` and that the resolved dependency graph
     has no SNAPSHOT/dynamic versions (`checkNoSnapshotDependencies`).
  2. Determines the previous released version from this repository's own GitHub tags
     (there is no Maven registry to query), and resolves whether a comparable, previously
     published `toolchain-logging-X.Y.Z.jar` baseline asset exists (the release
     CLI's `resolve-japicmp-baseline`, in `Qilletni/Qilletni`'s `tools/release`)
     - **never fabricating one**. The very
     first release under this scheme has no baseline yet, so japicmp is skipped for it;
     every release publishes its own `toolchain-logging-X.Y.Z.jar` asset so the *next*
     release has one.
  3. When a baseline exists, runs `toolchain-logging`'s japicmp check and enforces the
     patch/minor/major policy (preferring the release marker's recorded bump kind), same
     rules as `Qilletni/Qilletni`:
     - **patch**: rejects *any* additive or breaking public API change.
     - **minor**: rejects breaking changes; additive changes are allowed.
     - **major**: breaking changes are allowed only if `docs/migrations/X.Y.Z.md` exists.
  4. Builds the shadowed CLI jar, the `toolchain-logging` jar, a CycloneDX JSON SBOM, and
     `component-manifest.json` (this CLI's own version, the exact embedded
     `qilletni-core`/`qilletni-api`/`qilletni-pkgutil`/`qilletni-docgen` versions, and the
     source commit - see the `generateComponentManifest` Gradle task). The manifest is
     packaged **inside** the release archive as well as attached as its own asset.
  5. Packages `qilletni-X.Y.Z.tar.gz` (jar + `component-manifest.json` + launcher scripts),
     computes its SHA-256, and creates the GitHub Release with the archive, the shadowed
     jar, the `toolchain-logging` jar, the SBOM and the component manifest all attached as
     assets.
- **`platform-dispatch`** (after `build-and-publish`): builds the exact
  `qilletni-platform-component-release` payload documented centrally (this repository,
  version/tag/full commit/archive name + SHA-256, and every embedded dependency version),
  live-re-verifies it against the just-published GitHub release
  (`verify-release-event-provenance`), mints a GitHub App token scoped to **only**
  `Qilletni/Qilletni` (never this repository's own token, never an org-wide token), and
  dispatches a `qilletni-platform-component-release` `repository_dispatch` event to it, so
  a reviewed platform-candidate PR is opened centrally.
- **`snapshot-followup`** (after `platform-dispatch`): opens a follow-up PR bumping
  `toolchainVersion` to `X.Y.(Z+1)-SNAPSHOT` and removing the consumed release marker, via
  `Qilletni/Qilletni/.github/workflows/reusable-snapshot-followup.yml@master`. Never
  auto-merges.

`qilletni --version` reports this CLI's own version alongside the exact embedded
`qilletni-core`/`qilletni-api`/`qilletni-pkgutil`/`qilletni-docgen` versions and the
source commit, read from the `version.properties` resource generated by the
`generateVersionInfo` Gradle task at build time (see `VersionProvider`).

### Consuming upstream dependency updates

`dependency-update.yml` receives a `repository_dispatch` event named
`qilletni-dependency-release` (sent by whichever upstream producer just released
`qilletni-core`, `qilletni-pkgutil` or `qilletni-docgen`) and forwards it, with this
repository's own App credentials, to
`Qilletni/Qilletni/.github/workflows/reusable-dependency-update.yml@master`, which
validates the payload against the `dependencies` mapping in `.qilletni/release.yml`,
re-verifies every artifact live against Maven Central, updates only the matching
`gradle.properties` key, refreshes Gradle dependency locks (`gradle.lockfile`,
`toolchain-logging/gradle.lockfile`) with sibling composite builds explicitly disabled,
runs the full test suite and `checkNoSnapshotDependencies`, confirms the resolved
dependency graph really contains the requested version, and opens a signed,
**never-auto-merging** PR.

### Authentication

Cross-repository dispatch and every PR this automation opens authenticate as a GitHub App
(organization secrets `QILLETNI_RELEASE_APP_ID` / `QILLETNI_RELEASE_APP_PRIVATE_KEY`) via
`actions/create-github-app-token@v3`, each token scoped to exactly one target repository -
never a broad, org-wide token.

### Dependency locking

`gradle.lockfile` and `toolchain-logging/gradle.lockfile` pin the exact resolved
dependency graph used to build a release. Regenerate both after a dependency change:

```bash
./gradlew dependencies --write-locks -PincludeSiblingBuilds=false
./gradlew :toolchain-logging:dependencies --write-locks -PincludeSiblingBuilds=false
```

### PR verification

`pr-ci.yml` runs `./gradlew clean build checkNoSnapshotDependencies` (sibling composite
builds explicitly disabled) and validates `.qilletni/release.yml` against every pull
request opened on this repository - ordinary change PRs, automated `dependency-update`
PRs and automated `release` PRs alike.
