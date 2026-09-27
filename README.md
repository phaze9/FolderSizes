# Folder Sizes

An IntelliJ Platform plugin that adds recursive directory details to every local folder in the **Project** view:

```text
src  12 dirs, 84 files, 1.27 MB
```

The counts include all descendant directories and files. Symbolic links are counted as files and are not followed.

## How it works

- Decoration never performs disk I/O on the UI thread. A missing value is shown as `calculating…` while a background scan runs.
- One bottom-up scan publishes metrics for the requested directory and its descendants, avoiding a separate recursive walk when child folders are expanded.
- Results are held in a per-project cache capped at 50,000 entries and expire after 10 minutes.
- IntelliJ VFS events invalidate changed paths, their descendants, and cached ancestors, then refresh the Project view after a short debounce.
- **Recalculate Folder Sizes** in the Project view context menu clears the cache on demand.
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
