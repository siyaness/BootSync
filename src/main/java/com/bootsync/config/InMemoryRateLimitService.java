package com.bootsync.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Service;

@Service
public class InMemoryRateLimitService {

    private final ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimitService(Clock clock) {
        this.clock = clock;
    }

    public boolean tryConsume(String key, int maxCount, Duration window) {
        Deque<Instant> events = bucket(key);
        Instant now = Instant.now(clock);
        prune(events, now.minus(window));
        if (events.size() >= maxCount) {
            return false;
        }
        events.addLast(now);
        return true;
    }

    public boolean isExceeded(String key, int maxCount, Duration window) {
        Deque<Instant> events = bucket(key);
        prune(events, Instant.now(clock).minus(window));
        return events.size() >= maxCount;
    }

    public void record(String key) {
        bucket(key).addLast(Instant.now(clock));
    }

    public void clear(String key) {
        buckets.remove(key);
    }

    public void clearAll() {
        buckets.clear();
    }

    private Deque<Instant> bucket(String key) {
        return buckets.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
    }

    private void prune(Deque<Instant> events, Instant threshold) {
        while (!events.isEmpty() && events.peekFirst().isBefore(threshold)) {
            events.pollFirst();
        }
    }
}
