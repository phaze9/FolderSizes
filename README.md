# Folder Sizes

An IntelliJ Platform plugin that adds recursive directory details to every local folder and module grouping node in the **Project** view:

```text
src  12 dirs, 84 files, 1.27 MB
```

The counts include all descendant directories and files. Symbolic links are counted as files and are not followed.
Module details combine the recursive metrics of all the module's content roots.
Folder details follow the Project view's **File Details** toggle. The plugin enables
File Details automatically when it is first installed or re-enabled; turning File
Details off afterwards also hides the folder details.

## How it works

- Decoration never performs disk I/O on the UI thread. A missing value is shown as `calculating…` while a background scan runs.
- One bottom-up scan publishes metrics for the requested directory and its descendants, avoiding a separate recursive walk when child folders are expanded.
- Results are held in a per-project cache capped at 50,000 entries and persisted in IntelliJ's project cache. Reopened projects show their last known values immediately while visible restored entries are refreshed in the background. Live entries expire after 10 minutes and are recalculated on their next request.
- IntelliJ VFS events update cached ancestors incrementally when they provide exact size information (content changes, ordinary file creation/deletion, empty directories, and known subtree moves/renames). Ambiguous events invalidate only the affected paths and ancestors. The Project view refresh is debounced in both cases.
- **Recalculate Folder Sizes** in the Project view context menu and Options → Appearance clears the cache on demand.
- **Size** in the Project view's Sort menu orders files, folders, and modules from largest to smallest. Module size is the combined recursive size of its content roots. Folders and modules move into place as their background calculations finish.
- Unreadable entries do not abort a scan; the displayed result is marked `partial`.

## Build and run

The project targets IntelliJ IDEA 2026.2 (build 262) and requires Java 25 and Gradle 9 or newer.

```bash
./gradlew test
./gradlew runIde
./build-plugin.sh
```

The installable ZIP is produced under `build/distributions/`.
`build-plugin.sh` removes the large `.gradle-user` cache when the build finishes,
including after a failed or interrupted build, while preserving the plugin ZIP,
`.gradle`, and `.intellijPlatform`.

Every push to `main` is tested and built by GitHub Actions. After a successful
build, the workflow creates a uniquely tagged GitHub release and attaches the
installable ZIP. The workflow can also be run manually from the Actions page.
