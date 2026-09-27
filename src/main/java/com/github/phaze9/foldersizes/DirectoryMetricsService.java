package com.github.phaze9.foldersizes;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service(Service.Level.PROJECT)
public final class DirectoryMetricsService implements Disposable {
    private static final long MAX_CACHE_AGE_NANOS = Duration.ofMinutes(10).toNanos();
    private static final int MAX_CACHE_ENTRIES = 50_000;
    private static final long REFRESH_DELAY_MILLIS = 150;

    private final Project project;
    private final ConcurrentHashMap<Path, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Set<Path> inFlight = new HashSet<>();
    private final Set<String> sizeSortedPanes = ConcurrentHashMap.newKeySet();
    private final Object inFlightLock = new Object();
    private final AtomicLong generation = new AtomicLong();
    private final AtomicBoolean refreshScheduled = new AtomicBoolean();

    public DirectoryMetricsService(Project project) {
        this.project = project;
    }

    public static DirectoryMetricsService getInstance(Project project) {
        return project.getService(DirectoryMetricsService.class);
    }

    @Nullable
    DirectoryMetrics getOrSchedule(VirtualFile directory) {
        Path path = localDirectoryPath(directory);
        if (path == null) {
            return null;
        }

        long currentGeneration = generation.get();
        CacheEntry entry = cache.get(path);
        if (entry != null
                && System.nanoTime() - entry.createdAtNanos <= MAX_CACHE_AGE_NANOS) {
            return entry.metrics;
        }
        if (entry != null) {
            cache.remove(path, entry);
        }
        scheduleScan(path, currentGeneration);
        return null;
    }

    @Nullable
    Long getSizeOrSchedule(VirtualFile file) {
        if (!file.isValid() || !file.isInLocalFileSystem()) {
            return null;
        }
        if (!file.isDirectory()) {
            return file.getLength();
        }

        DirectoryMetrics metrics = getOrSchedule(file);
        return metrics == null ? null : metrics.totalBytes();
    }

    @Nullable
    Long getCombinedSizeOrSchedule(Collection<VirtualFile> roots) {
        DirectoryMetrics metrics = getCombinedMetricsOrSchedule(roots);
        return metrics == null ? null : metrics.totalBytes();
    }

    @Nullable
    DirectoryMetrics getCombinedMetricsOrSchedule(Collection<VirtualFile> roots) {
        DirectoryMetrics combined = new DirectoryMetrics(0, 0, 0, true);
        boolean available = true;
        Set<VirtualFile> uniqueRoots = new HashSet<>(roots);
        for (VirtualFile root : uniqueRoots) {
            DirectoryMetrics metrics = getOrSchedule(root);
            if (metrics == null) {
                available = false;
            } else {
                combined = combined.plus(metrics);
            }
        }
        return available ? combined : null;
    }

    void pathsChanged(Collection<Path> changedPaths) {
        if (changedPaths.isEmpty()) {
            return;
        }

        Set<Path> normalized = new HashSet<>();
        for (Path path : changedPaths) {
            if (path != null) {
                normalized.add(path.toAbsolutePath().normalize());
            }
        }
        if (normalized.isEmpty() || !isRelevant(normalized)) {
            return;
        }

        generation.incrementAndGet();
        cache.keySet().removeIf(cached -> normalized.stream().anyMatch(changed ->
                cached.startsWith(changed) || changed.startsWith(cached)));
        scheduleProjectViewRefresh();
    }

    public void invalidateAll() {
        generation.incrementAndGet();
        cache.clear();
        scheduleProjectViewRefresh();
    }

    boolean isSizeSortingEnabled(String paneId) {
        return sizeSortedPanes.contains(paneId);
    }

    void setSizeSortingEnabled(String paneId, boolean enabled) {
        if (enabled) {
            sizeSortedPanes.add(paneId);
        } else {
            sizeSortedPanes.remove(paneId);
        }
    }

    private void scheduleScan(Path root, long scanGeneration) {
        synchronized (inFlightLock) {
            boolean coveredByExistingScan = inFlight.stream().anyMatch(root::startsWith);
            if (coveredByExistingScan || project.isDisposed()) {
                return;
            }
            inFlight.add(root);
        }

        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                DirectoryTreeScanner.scan(root, (directory, metrics) -> {
                    if (generation.get() == scanGeneration) {
                        putBounded(directory, metrics, root);
                    }
                });
            } catch (IOException | SecurityException ignored) {
                // A directory can disappear while it is being scanned. A later decoration or VFS event retries it.
            } finally {
                synchronized (inFlightLock) {
                    inFlight.remove(root);
                }
                if (generation.get() == scanGeneration) {
                    scheduleProjectViewRefresh();
                }
            }
        });
    }

    private void putBounded(Path directory, DirectoryMetrics metrics, Path requestedRoot) {
        if (cache.size() >= MAX_CACHE_ENTRIES && !cache.containsKey(directory)) {
            if (!directory.equals(requestedRoot)) {
                return;
            }
            // The explicitly requested result is more useful than an arbitrary descendant entry.
            cache.keySet().stream().findAny().ifPresent(cache::remove);
        }
        cache.put(directory, new CacheEntry(metrics, System.nanoTime()));
    }

    private boolean isRelevant(Set<Path> changedPaths) {
        String basePath = project.getBasePath();
        if (basePath != null) {
            Path base = Path.of(basePath).toAbsolutePath().normalize();
            if (changedPaths.stream().anyMatch(path -> path.startsWith(base) || base.startsWith(path))) {
                return true;
            }
        }
        return cache.keySet().stream().anyMatch(cached -> changedPaths.stream().anyMatch(path ->
                cached.startsWith(path) || path.startsWith(cached)));
    }

    private void scheduleProjectViewRefresh() {
        if (!refreshScheduled.compareAndSet(false, true)) {
            return;
        }
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            refreshScheduled.set(false);
            if (project.isDisposed()) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    ProjectView.getInstance(project).refresh();
                }
            });
        }, REFRESH_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Nullable
    private static Path localDirectoryPath(VirtualFile file) {
        if (!file.isValid() || !file.isDirectory() || !file.isInLocalFileSystem()) {
            return null;
        }
        try {
            return file.toNioPath().toAbsolutePath().normalize();
        } catch (UnsupportedOperationException | IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public void dispose() {
        generation.incrementAndGet();
        cache.clear();
        sizeSortedPanes.clear();
        synchronized (inFlightLock) {
            inFlight.clear();
        }
    }

    private record CacheEntry(DirectoryMetrics metrics, long createdAtNanos) {
    }
}
