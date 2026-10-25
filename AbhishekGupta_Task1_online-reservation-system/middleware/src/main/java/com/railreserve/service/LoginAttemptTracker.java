package com.railreserve.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Basic brute-force protection for the login screen: after
 * {@value DEFAULT_MAX_FAILURES} consecutive failed attempts the account lock
 * engages for {@value DEFAULT_LOCKOUT_SECONDS} seconds. Pure logic with an
 * injectable {@link Clock} so tests can time-travel without sleeping.
 */
public class LoginAttemptTracker {

    public static final int DEFAULT_MAX_FAILURES = 5;
    public static final int DEFAULT_LOCKOUT_SECONDS = 30;

    private final int maxFailures;
    private final int lockoutSeconds;
    private final Clock clock;

    private int consecutiveFailures;
    private Instant lockedUntil;

    public LoginAttemptTracker() {
        this(DEFAULT_MAX_FAILURES, DEFAULT_LOCKOUT_SECONDS, Clock.systemDefaultZone());
    }

    /** Test seam: maxFailures, lockout seconds and a controllable clock. */
    public LoginAttemptTracker(int maxFailures, int lockoutSeconds, Clock clock) {
        this.maxFailures = maxFailures;
        this.lockoutSeconds = lockoutSeconds;
        this.clock = clock;
    }

    /** True while the lockout is in effect. */
    public boolean isLocked() {
        return lockedUntil != null && Instant.now(clock).isBefore(lockedUntil);
    }

    /** Seconds until the lock clears (0 when unlocked). */
    public long remainingSeconds() {
        if (lockedUntil == null) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(clock), lockedUntil).getSeconds();
        return Math.max(0, seconds);
    }

    /** Registers a failed attempt; may engage the lock. */
    public void recordFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= maxFailures) {
            lockedUntil = Instant.now(clock).plusSeconds(lockoutSeconds);
        }
    }

    /** Successful login clears the count and any lock request state. */
    public void recordSuccess() {
        consecutiveFailures = 0;
        lockedUntil = null;
    }

    /** Clears the lock once its window elapsed so counting can restart cleanly. */
    public void resetIfLockExpired() {
        if (lockedUntil != null && !isLocked()) {
            consecutiveFailures = 0;
            lockedUntil = null;
        }
    }
}
