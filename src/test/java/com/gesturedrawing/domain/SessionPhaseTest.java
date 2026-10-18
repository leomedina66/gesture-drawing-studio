package com.gesturedrawing.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionPhaseTest {

    @Test
    void shouldFormatDurations() {
        assertEquals("30s", SessionPhase.formatDuration(30));
        assertEquals("1 min", SessionPhase.formatDuration(60));
        assertEquals("1 min 30s", SessionPhase.formatDuration(90));
        assertEquals("10 min", SessionPhase.formatDuration(600));
        assertEquals("1 h 5 min", SessionPhase.formatDuration(3900));
        assertEquals("0s", SessionPhase.formatDuration(0));
        assertEquals("5 × 2 min", new SessionPhase(5, 120).toString());
    }

    @Test
    void shouldRoundTripThroughSerialization() {
        List<SessionPhase> plan = List.of(new SessionPhase(2, 60), new SessionPhase(5, 120), new SessionPhase(1, 600));

        assertEquals("2x60;5x120;1x600", SessionPhase.serialize(plan));
        assertEquals(plan, SessionPhase.parse(SessionPhase.serialize(plan)));
    }

    @Test
    void parseShouldSkipMalformedEntries() {
        assertEquals(List.of(new SessionPhase(3, 45)), SessionPhase.parse("3x45;oops;0x60;2x;x30;4x-1"));
        assertTrue(SessionPhase.parse("1x1;2x2000000000;501x60").isEmpty(), "Out-of-range values from prefs are dropped");
        assertTrue(SessionPhase.parse(null).isEmpty());
        assertTrue(SessionPhase.parse("  ").isEmpty());
    }

    @Test
    void shouldRejectInvalidPhases() {
        assertThrows(IllegalArgumentException.class, () -> new SessionPhase(0, 60));
        assertThrows(IllegalArgumentException.class, () -> new SessionPhase(3, 0));
        assertEquals(600, new SessionPhase(5, 120).totalSeconds());
    }
}
