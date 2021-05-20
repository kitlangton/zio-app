# zio-app

[![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

Quickly create and develop full-stack Scala apps with ZIO and Laminar.

## Installation

```sh
brew tap kitlangton/zio-app
brew install zio-app
```

## Usage

1. Create a new project

```sh
zio-app new

# Configure your new ZIO app.
# ? Project Name (example) zio-app-example

cd zio-app-example
```

2. Launch file-watching compilation and hot-reloading dev server:

```sh
zio-app dev

# Launches:
┌───────────────────────────────────────────────────────────┐
│ zio-app                  running at http://localhost:3000 │
└───────────────────────────INFO────────────────────────────┘
┌────────────────────────────┐┌─────────────────────────────┐
│                            ││                             │
│                            ││                             │
│[info] welcome to sbt 1.5.2 ││[info] welcome to sbt 1.5.2 (│
│[info] loading global plugin││[info] loading global plugins│
│[info] loading settings for ││[info] loading settings for p│
│[info] loading project defin││[info] loading project defini│
│[info] loading settings for ││[info] loading settings for p│
│[info] set current project t││[info] set current project to│
│[warn] sbt server could not ││[warn] sbt server could not s│
│[warn] Running multiple inst││[warn] Running multiple insta│
│[info] compiling 6 Scala sou││[info] compiling 6 Scala sour│
│[info] done compiling       ││[info] done compiling        │
│[info] compiling 12 Scala so││[info] compiling 3 Scala sour│
└──────────FRONTEND──────────┘└───────────BACKEND───────────┘
```

----

[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/io.github.kitlangton/zio-app_2.13.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/io.github.kitlangton/zio-app_2.13.svg "Sonatype Snapshots"
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/io/github/kitlangton/zio-app_2.13/ "Sonatype Snapshots"
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/releases/io/github/kitlangton/zio-app_2.13/ "Sonatype Releases"
