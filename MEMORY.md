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

- `DirectoryMetricsDecorator` preserves the node's original name as the first colored-text fragment, then adds the Project view suffix; it never performs disk I/O on the UI thread. Local directory-backed nodes use their cached recursive metrics, while actual `Module` values combine the metrics of their content roots.
- `ModuleContentRoots` resolves content roots for both `Module` and `ModuleGroup` values. Groups include modules from descendant groups, and nested content roots are collapsed before aggregation so files are not counted twice.
- IntelliJ does not invoke `ProjectViewNodeDecorator` for its synthetic `ModuleGroupNode` rows. `ModuleGroupMetricsTreeStructureProvider` therefore replaces `ProjectViewModuleGroupNode` instances with `MetricsModuleGroupNode`; that wrapper appends the same shared metrics suffix from its `update()` method. The provider is registered through the supported project-level `com.intellij.treeStructureProvider` extension point.
- Folder metrics are shown only while IntelliJ's **File Details** option is enabled. That action is backed by `UISettings.showInplaceComments` in IDEA 2026.2.3.
- `FolderSizesPluginListener` enables File Details on first installation and after a dynamic plugin re-enable. An application property records an active installation so ordinary IDE restarts do not override a user's later manual choice; plugin unload clears the property for the next activation.
- `DirectoryMetricsService` owns the per-project cache, persists it through IntelliJ's project cache storage, schedules background scans, coalesces Project view refreshes, handles manual invalidation, and removes descendant roots before combining multi-root metrics. `DirectoryMetricsState` is the XML-serializable snapshot model.
- `DirectoryTreeScanner` performs a bottom-up NIO tree walk. A single scan publishes results for the requested root and its descendant directories.
- `DirectoryChangeListener` uses IntelliJ VFS event state to maintain cached totals incrementally when a change is exact: content-length changes, ordinary file creation/deletion, empty-directory creation, and cached subtree moves/renames. It snapshots deletions and moves in `before()` while the old `VirtualFile` state is still valid. Events without enough trustworthy information fall back to invalidating the affected subtree and cached ancestors.
- `RecalculateFolderSizesAction` exposes **Recalculate Folder Sizes** in the Project view context menu and under **Options → Appearance**, and clears the cache.
- `SortBySizeAction` exposes **Size** in the Project view's native **Sort By** menu. It installs `SizeComparator` for the active pane and restores IntelliJ's normal comparator when disabled.
- `SizeComparator` orders known sizes largest-first, leaves names as the stable tie-breaker, and respects **Folders Always on Top**. Files use their `VirtualFile` length, folders use cached recursive metrics, and modules and module groups use the combined recursive size of their unique content roots.
- `MetricFormatter` formats decimal units (`KB`, `MB`, and so on) and singular/plural counts.

## Cache and recalculation rules

- Cache scope is one IntelliJ project.
- Maximum cache size is 50,000 directory entries.
- IntelliJ persists the bounded cache in its project cache storage. On project reopen, saved metrics display immediately and are treated like current in-memory entries.
- IntelliJ's VFS refresh events update or invalidate only the affected cache entries, both when opening a project and when the user refreshes the Project view. Automatically invalidated entries retain their last known metrics while replacement scans run.
- In-memory entries expire after 24 hours. Expired entries likewise retain their last known metrics until the replacement scan completes. Expiry remains a correctness backstop for missed, ambiguous, or external filesystem changes; it is not a scheduled refresh.
- **Recalculate Folder Sizes** remains the explicit full-cache refresh and is the only refresh that clears displayed metrics to `calculating…` while scans run.
- Concurrent scans are deduplicated when an existing ancestor scan already covers a requested directory.
- A generation counter prevents results from an in-progress scan being published after a relevant VFS change.
- Exact VFS deltas update complete, unsaturated ancestor entries in place. Any underflow, overflow, saturated value, partial result, symlink/special-file event, or unknown subtree contribution falls back to lazy recalculation rather than risking an incorrect total.
- When a generation change makes an in-progress scan stale, completion still refreshes the Project view after removing the in-flight marker. This lets a visible node immediately request a replacement scan instead of remaining on `calculating…`.
- Project view refreshes are debounced by 150 ms.
- Missing metrics display `calculating…` until the background scan completes.
- Sort-by-size requests missing folder and module metrics through the same background scan path. Unknown entries sort after known entries and move into place on the normal debounced Project view refresh.
- Module and module-group decoration and size sorting share `DirectoryMetricsService.getCombinedMetricsOrSchedule`, so counts, total size, loading state, and partial status are derived from the same content-root results. Descendant content roots are removed before aggregation to prevent double-counting nested modules.
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

After every verified plugin code change, deploy the current ZIP to the user
plugin directory rather than leaving IntelliJ on the previous build:

```bash
mkdir -p "$HOME/Library/Application Support/JetBrains/IntelliJIdea2026.2/plugins"
/usr/bin/ditto -x -k build/distributions/folder-sizes-1.0.0.zip \
  "$HOME/Library/Application Support/JetBrains/IntelliJIdea2026.2/plugins"
```

Verify the installed JAR byte-for-byte against the JAR inside the ZIP. Restart
IntelliJ, or dynamically disable and re-enable the plugin, before expecting the
running IDE to use the new classes.

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

## JetBrains Marketplace

Version `1.0.0` was submitted to JetBrains Marketplace on 2026-09-27 as
**Folder Sizes**:

- Marketplace page: <https://plugins.jetbrains.com/plugin/34576-folder-sizes>
- Plugin ID: `com.github.phaze9.foldersizes`
- Public vendor name: `Ivo Sele`
- Vendor ID: `phaze9`
- Vendor status: non-trader
- License: MIT; the repository root `LICENSE` contains the matching license
- Source and plugin homepage: <https://github.com/phaze9/FolderSizes>
- Channel: Stable
- Marketplace tag: Folder
- Ads: none
- Visibility after approval: public

The submitted `build/distributions/folder-sizes-1.0.0.zip` has SHA-256
`1051552692fc556eab3c2e0687595388a47beed8b73f8e37e2f626207be88a81`.
Before upload, all tests passed and Plugin Verifier reported `Compatible` for
IDEA builds `262.10968.63` and `263.5701.42`, with dynamic enable/disable
eligibility.

The first upload succeeded and is awaiting JetBrains's final checks. The upload
confirmation said review should complete within two business days. Until that
approval completes, the Marketplace page exists but the plugin is not public.

Keep all email addresses out of tracked repository files and plugin metadata.
The Marketplace vendor email is private. Direct public support and user
communication to GitHub only.

On 2026-09-27:

- All 5 unit tests passed, including regression coverage that keeps folder names visible without duplicating names that IntelliJ already supplied as colored text.
- JetBrains Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`.
- After tying folder metrics to File Details and adding automatic activation, `test buildPlugin` passed and Plugin Verifier again reported `Compatible` for both builds with no plugin defects or API warnings. The plugin remained eligible for dynamic enable/disable without an IDE restart.
- After adding size sorting for files, folders, and modules, all 7 unit tests and `buildPlugin` passed. Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`, with no internal or experimental API warnings; dynamic enable/disable remained eligible.
- An initial decorator-only attempt at recursive module-group stats passed all 9 unit tests and Plugin Verifier, but it did not change the synthetic `ModuleGroupNode` rows because IntelliJ never invokes `ProjectViewNodeDecorator` for those nodes.
- After placing recalculation under **Options → Appearance** and shortening the native **Sort By** entry to **Size**, all 9 unit tests and `buildPlugin` passed. The rebuilt JAR was installed and its action registrations were verified from the installed manifest.
- After adding the tree-structure provider and module-group node wrapper, all 10 unit tests passed. Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`, with no internal API usage; dynamic enable/disable remained eligible. The new regression test verifies that nested content roots are excluded from combined metrics.
- The rebuilt plugin was installed into the IntelliJ IDEA 2026.2 user-plugin directory. Its JAR matched the build output at SHA-256 `e9692a0f076468173418a5b9dcfd4f73d2d3051cb3c764aad9037826b4630013`.
- After a full IntelliJ restart, the live `$HOME/Sites` Project view showed stats on the grouping rows, including `gewinnspiele-test` (29 dirs, 2,231 files, 212 MB), `kittybreeder` (420 dirs, 4,088 files, 49.8 MB), and `liv` (3,226 dirs, 36,628 files, 870 MB).
- Incremental VFS cache synchronization was added while retaining the lazy bottom-up scan as the baseline and fallback. All 12 unit tests passed, and Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`, with dynamic enable/disable eligibility.
- The incremental build was deployed to the IDEA 2026.2 user-plugin directory and verified against the packaged JAR at SHA-256 `8865cdeca22b79b7f891c09ffa0398e926ed1d31e8763ed0811aa8360b988cde`. The distributable ZIP SHA-256 was `84cfae03f208622100ef48d66cb185a2ef76d569ddeaa9d19b6bfd585a7d4037`.
- Project-scoped cache persistence was added with stale-while-revalidate behavior. Reopened projects show saved metrics immediately, visible entries refresh in the background, and unrefreshed restored values are excluded from exact VFS subtree arithmetic. IntelliJ XML serialization has a direct round-trip regression test.
- All 16 unit tests passed. Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`, with dynamic enable/disable eligibility.
- The persistent-cache build was deployed to the IDEA 2026.2 user-plugin directory. Its installed JAR matched the packaged JAR at SHA-256 `808e87178b00a744aff4d41bd96f2f9714e17930911f2a40752898101f216984`; the distributable ZIP SHA-256 was `2eb86e9e8e5852ea755bb0145b9a9805a1fb51911d5735efc7e170bd6885cbbb`.
- The cache lifetime was increased from 10 minutes to 24 hours. Restored entries are accepted immediately, while IntelliJ startup and Project view refreshes update or invalidate only paths reported through VFS events. Automatic expiry and ambiguous-event invalidation use stale-while-refresh behavior so existing metrics remain visible until replacements are ready; only **Recalculate Folder Sizes** clears values to `calculating…`.
- All 16 unit tests passed, and Plugin Verifier reported `Compatible` for IDEA builds `262.10968.63` and `263.5701.42`, with dynamic enable/disable eligibility. The build was deployed locally; its installed JAR matched the packaged JAR at SHA-256 `9eb732d536398582d95920d0466810f4828db7494f7949ac916e43284ce5980c`, and the distributable ZIP SHA-256 was `96146fdfdc05729bfe062368ec3277e15ca6d32005b57b18d02ca393d201123f`.

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
