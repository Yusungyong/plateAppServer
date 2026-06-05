package com.plateapp.plate_main.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final int MAX_BUCKETS_BEFORE_CLEANUP = 20_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void check(String key, int maxAttempts, Duration window) {
        if (key == null || key.isBlank() || maxAttempts <= 0 || window == null || window.isNegative() || window.isZero()) {
            return;
        }

        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
        synchronized (bucket) {
            while (!bucket.attempts.isEmpty() && bucket.attempts.peekFirst() < cutoff) {
                bucket.attempts.removeFirst();
            }
            bucket.lastSeen = now;
            if (bucket.attempts.size() >= maxAttempts) {
                throw new RateLimitException("Too many requests. Please try again later.");
            }
            bucket.attempts.addLast(now);
        }

        if (buckets.size() > MAX_BUCKETS_BEFORE_CLEANUP) {
            cleanup(cutoff);
        }
    }

    public String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    public String identity(String value) {
        if (value == null || value.isBlank()) {
            return "blank";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void cleanup(long cutoff) {
        buckets.entrySet().removeIf(entry -> entry.getValue().lastSeen < cutoff);
    }

    private static final class Bucket {
        private final Deque<Long> attempts = new ArrayDeque<>();
        private volatile long lastSeen = System.currentTimeMillis();
    }
}
