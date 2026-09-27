# FolderSizes Project Memory

## Purpose

FolderSizes is an IntelliJ Platform plugin that decorates local directories in the Project view with recursive metrics:

```text
src  12 dirs, 84 files, 1.27 MB
```

Directory and file counts include all descendants. Symbolic links are counted as files and are not followed.

## Repository and target

- GitHub: <https://github.com/phaze9/FolderSizes>
- Plugin ID: `com.github.phaze9.foldersizes`
- Current version: `1.0.0`
- IntelliJ target: IDEA 2026.2.3, build 262
- Java toolchain: 25
- Gradle wrapper: 9.1.0
- IntelliJ Platform Gradle Plugin: 2.19.0

The development and installed target on this Mac is `/Applications/IntelliJ IDEA.app`, currently build `262.10968.63`. The plugin is installed in the corresponding per-user directory:

```text
~/Library/Application Support/JetBrains/IntelliJIdea2026.2/plugins/folder-sizes
```

Do not install it into the application bundle itself.

## Architecture

- `DirectoryMetricsDecorator` adds the Project view suffix and never performs disk I/O on the UI thread.
- `DirectoryMetricsService` owns the per-project cache, schedules background scans, coalesces Project view refreshes, and handles manual invalidation.
- `DirectoryTreeScanner` performs a bottom-up NIO tree walk. A single scan publishes results for the requested root and its descendant directories.
- `DirectoryChangeListener` converts VFS changes into affected paths. Changed subtrees and cached ancestors are invalidated; unrelated cached entries remain valid.
- `RecalculateFolderSizesAction` exposes **Recalculate Folder Sizes** in the Project view context menu and clears the cache.
- `MetricFormatter` formats decimal units (`KB`, `MB`, and so on) and singular/plural counts.

## Cache and recalculation rules

- Cache scope is one IntelliJ project.
- Maximum cache size is 50,000 directory entries.
- Entries expire after 10 minutes.
- Concurrent scans are deduplicated when an existing ancestor scan already covers a requested directory.
- A generation counter prevents results from an in-progress scan being published after a relevant VFS change.
- Project view refreshes are debounced by 150 ms.
- Missing metrics display `calculating…` until the background scan completes.
- Unreadable paths do not abort the entire walk; affected totals are labeled `partial`.

## Build and verification

Normal commands:

```bash
./gradlew test
./gradlew buildPlugin
./gradlew verifyPlugin
./gradlew runIde
```

The installable artifact is generated at:

```text
build/distributions/folder-sizes-1.0.0.zip
```

On 2026-09-27:

- All 3 unit tests passed.
- JetBrains Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`.
- The installed JAR was checked byte-for-byte against the JAR inside the distribution ZIP.

After copying a new build into the user plugin directory, restart IntelliJ to load it.

## Repository hygiene

Generated state must remain untracked. In particular, keep these ignored:

- `.gradle/`
- `.gradle-user/`
- `.intellijPlatform/`
- `.idea/`
- `build/`
- `out/`

The Gradle wrapper JAR and scripts are intentionally tracked.
