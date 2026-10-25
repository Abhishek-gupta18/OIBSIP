package com.railreserve.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure lockout logic with a time-travel clock — no sleeps. */
class LoginAttemptTrackerTest {

    /** Test clock whose instant the test controls directly. */
    private static final class MutableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T10:00:00Z"));

        @Override public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override public Instant instant() {
            return now.get();
        }

        void advance(Duration d) {
            now.set(now.get().plus(d));
        }
    }

    @Test
    void lockEngagesOnlyAfterConfiguredFailures() {
        MutableClock clock = new MutableClock();
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 30, clock);

        assertTrue(!tracker.isLocked());
        tracker.recordFailure();
        tracker.recordFailure();
        tracker.recordFailure();
        tracker.recordFailure();
        assertTrue(!tracker.isLocked(), "4 failures must not lock");
        tracker.recordFailure();
        assertTrue(tracker.isLocked(), "5th failure must engage the lock");
        assertEquals(30, tracker.remainingSeconds());
    }

    @Test
    void lockExpiresAfterWindow() {
        MutableClock clock = new MutableClock();
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 30, clock);
        for (int i = 0; i < 5; i++) {
            tracker.recordFailure();
        }
        clock.advance(Duration.ofSeconds(29));
        assertTrue(tracker.isLocked(), "still locked at 29s");
        clock.advance(Duration.ofSeconds(2));
        assertTrue(!tracker.isLocked(), "unlocked after 31s");
        tracker.resetIfLockExpired();
        assertEquals(0, tracker.remainingSeconds());
    }

    @Test
    void successResetsFailureCount() {
        MutableClock clock = new MutableClock();
        LoginAttemptTracker tracker = new LoginAttemptTracker(5, 30, clock);
        tracker.recordFailure();
        tracker.recordFailure();
        tracker.recordSuccess();
        for (int i = 0; i < 4; i++) {
            tracker.recordFailure();
        }
        assertTrue(!tracker.isLocked(), "counter restarted after success — 4 failures only");
        tracker.recordFailure();
        assertTrue(tracker.isLocked());
    }

    @Test
    void remainingSecondsNeverNegative() {
        MutableClock clock = new MutableClock();
        LoginAttemptTracker tracker = new LoginAttemptTracker(2, 10, clock);
        tracker.recordFailure();
        tracker.recordFailure();
        clock.advance(Duration.ofHours(1));   // way past the lock
        assertEquals(0, tracker.remainingSeconds());
    }
}
