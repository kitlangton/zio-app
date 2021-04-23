# zio-app

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
│[info] welcome to sbt 1.4.7 ││[info] welcome to sbt 1.4.7 (│
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
