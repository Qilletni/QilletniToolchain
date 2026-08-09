# Migration guides

A `major` release of `qilletni-toolchain` (see `RELEASE.md`) must be
accompanied by a migration guide at `docs/migrations/X.Y.Z.md`, named after
the exact new version being released. Central release preparation
(`reusable-release-prepare.yml`) refuses to prepare a `major` bump unless
this file already exists on `master` *before* release preparation runs, and
the publish workflow's japicmp gate accepts an otherwise-forbidden breaking
change to `toolchain-logging`'s public API only when this file is present
for the version being released (`check-japicmp-report --has-migration-doc`).

Each guide should cover, in plain language:

- What changed and why.
- The exact steps a consumer needs to take to upgrade (config changes,
  renamed/removed CLI options or `toolchain-logging` API usages, etc).
- Anything that cannot be done automatically.

There is no guide for `1.0.0` (the first release).
