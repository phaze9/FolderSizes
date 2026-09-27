# FolderSizes Project Memory

## Purpose

FolderSizes is an IntelliJ Platform plugin that decorates local directories and module grouping nodes in the Project view with recursive metrics:

```text
src  12 dirs, 84 files, 1.27 MB
```

Directory and file counts include all descendants. Symbolic links are counted as files and are not followed.
Module metrics combine the recursive metrics of all unique module content roots.

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

- `DirectoryMetricsDecorator` preserves the node's original name as the first colored-text fragment, then adds the Project view suffix; it never performs disk I/O on the UI thread. Local directory nodes use their cached recursive metrics, while module grouping nodes combine the metrics of all content roots.
- Folder metrics are shown only while IntelliJ's **File Details** option is enabled. That action is backed by `UISettings.showInplaceComments` in IDEA 2026.2.3.
- `FolderSizesPluginListener` enables File Details on first installation and after a dynamic plugin re-enable. An application property records an active installation so ordinary IDE restarts do not override a user's later manual choice; plugin unload clears the property for the next activation.
- `DirectoryMetricsService` owns the per-project cache, schedules background scans, coalesces Project view refreshes, and handles manual invalidation.
- `DirectoryTreeScanner` performs a bottom-up NIO tree walk. A single scan publishes results for the requested root and its descendant directories.
- `DirectoryChangeListener` converts VFS changes into affected paths. Changed subtrees and cached ancestors are invalidated; unrelated cached entries remain valid.
- `RecalculateFolderSizesAction` exposes **Recalculate Folder Sizes** in the Project view context menu and under **Options → Appearance**, and clears the cache.
- `SortBySizeAction` exposes **Size** in the Project view's native **Sort By** menu. It installs `SizeComparator` for the active pane and restores IntelliJ's normal comparator when disabled.
- `SizeComparator` orders known sizes largest-first, leaves names as the stable tie-breaker, and respects **Folders Always on Top**. Files use their `VirtualFile` length, folders use cached recursive metrics, and modules use the combined recursive size of all content roots.
- `MetricFormatter` formats decimal units (`KB`, `MB`, and so on) and singular/plural counts.

## Cache and recalculation rules

- Cache scope is one IntelliJ project.
- Maximum cache size is 50,000 directory entries.
- Entries expire after 10 minutes.
- Concurrent scans are deduplicated when an existing ancestor scan already covers a requested directory.
- A generation counter prevents results from an in-progress scan being published after a relevant VFS change.
- Project view refreshes are debounced by 150 ms.
- Missing metrics display `calculating…` until the background scan completes.
- Sort-by-size requests missing folder and module metrics through the same background scan path. Unknown entries sort after known entries and move into place on the normal debounced Project view refresh.
- Module decoration and size sorting share `DirectoryMetricsService.getCombinedMetricsOrSchedule`, so module counts, total size, loading state, and partial status are derived from the same content-root results.
- Unreadable paths do not abort the entire walk; affected totals are labeled `partial`.

## Build and verification

Normal commands:

```bash
./gradlew test
./build-plugin.sh
./gradlew verifyPlugin
./gradlew runIde
```

Use `build-plugin.sh` for distributable builds. It runs `buildPlugin` with
`.gradle-user` as an isolated Gradle user home and removes that directory when
the build exits, including after failures or interruptions. It deliberately
preserves `.gradle/`, `.intellijPlatform/`, and the ZIP under
`build/distributions/`.

This cleanup is necessary because IntelliJ platform dependencies previously
expanded `.gradle-user` to roughly 12 GiB (about 13 GB decimal), including two
downloaded IDEA images and their transformed installations. The plugin itself
remained small: about 52 KB of source and an 18–20 KB distributable ZIP.

The installable artifact is generated at:

```text
build/distributions/folder-sizes-1.0.0.zip
```

## Automated GitHub releases

`.github/workflows/release.yml` runs on every push to `main` and can also be
started manually. It sets up Java 25, runs `./gradlew --no-daemon test
buildPlugin`, requires exactly one ZIP under `build/distributions/`, and only
then creates a GitHub release with that ZIP attached.

Each successful run uses a unique `build-<run number>.<run attempt>` tag so a
rerun does not overwrite an earlier release. The release targets the exact
built commit and is marked as the latest release. Publishing uses the workflow's
`GITHUB_TOKEN` with `contents: write`; no separate release credential is needed.

The automation was introduced by commit `90d1740`. Its first run, GitHub Actions
run `36295682434`, completed successfully in 3 minutes 8 seconds and published
release `build-1.1`. The attached `folder-sizes-1.0.0.zip` was 18,392 bytes with
SHA-256 `c31e99e75682d2662c4baa5528210004c7278055b0f30b710a6eb2a47a3e6374`.

On 2026-09-27:

- All 5 unit tests passed, including regression coverage that keeps folder names visible without duplicating names that IntelliJ already supplied as colored text.
- JetBrains Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`.
- After tying folder metrics to File Details and adding automatic activation, `test buildPlugin` passed and Plugin Verifier again reported `Compatible` for both builds with no plugin defects or API warnings. The plugin remained eligible for dynamic enable/disable without an IDE restart.
- After adding size sorting for files, folders, and modules, all 7 unit tests and `buildPlugin` passed. Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`, with no internal or experimental API warnings; dynamic enable/disable remained eligible.
- After adding recursive stats to module grouping nodes, all 9 unit tests passed. Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`; dynamic enable/disable remained eligible. New aggregation coverage verifies count/size summation, partial-status propagation, and saturation at `Long.MAX_VALUE`.
- After placing recalculation under **Options → Appearance** and shortening the native **Sort By** entry to **Size**, all 9 unit tests and `buildPlugin` passed. The rebuilt JAR was installed and its action registrations were verified from the installed manifest.
- The rebuilt plugin was installed into the IntelliJ IDEA 2026.2 user-plugin directory, and the installed JAR was checked byte-for-byte against the build output.

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
