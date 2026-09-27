package com.github.phaze9.foldersizes;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DirectoryMetricsStateTest {
    @Test
    void restoresPersistedMetrics() {
        Path path = Path.of("project", "src").toAbsolutePath().normalize();
        DirectoryMetrics metrics = new DirectoryMetrics(3, 7, 1_024, false);

        DirectoryMetricsState.RestoredEntry restored =
                new DirectoryMetricsState.Entry(path, metrics).restore();

        assertEquals(path, restored.path());
        assertEquals(metrics, restored.metrics());
    }

    @Test
    void ignoresInvalidPersistedMetrics() {
        DirectoryMetricsState.Entry entry = new DirectoryMetricsState.Entry();
        entry.path = "project";
        entry.directoryCount = -1;

        assertNull(entry.restore());
    }

    @Test
    void defaultsToAnEmptySnapshot() {
        assertFalse(new DirectoryMetricsState().entries.iterator().hasNext());
    }

    @Test
    void roundTripsThroughIntellijStateSerialization() {
        Path path = Path.of("project", "src").toAbsolutePath().normalize();
        DirectoryMetrics metrics = new DirectoryMetrics(2, 5, 512, true);
        DirectoryMetricsState state = new DirectoryMetricsState();
        state.entries.add(new DirectoryMetricsState.Entry(path, metrics));

        Element serialized = XmlSerializer.serialize(state);
        DirectoryMetricsState loaded =
                XmlSerializer.deserialize(serialized, DirectoryMetricsState.class);

        DirectoryMetricsState.RestoredEntry restored = loaded.entries.getFirst().restore();
        assertEquals(path, restored.path());
        assertEquals(metrics, restored.metrics());
    }
}
