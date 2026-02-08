package com.plateapp.plate_main.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("backfill-store-name-naver")
@RequiredArgsConstructor
@Slf4j
public class StoreNameNaverBackfillRunner implements CommandLineRunner {

    private static final String SELECT_SQL = """
        SELECT s.store_id,
               s.place_id,
               COALESCE(s.address, p.formatted_address) AS address
        FROM fp_300 s
        LEFT JOIN fp_310 p ON s.place_id = p.place_id
        WHERE s.place_id IS NOT NULL
        """;

    private static final String UPDATE_SQL = """
        UPDATE fp_300
        SET store_name = :store_name
        WHERE store_id = :store_id
        """;

    private static final int MAX_NAME_LEN = 50;
    private static final String CSV_PATH = "naver-backfill-skipped.csv";

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Value("${naver.local-search.client-id:}")
    private String clientId;

    @Value("${naver.local-search.client-secret:}")
    private String clientSecret;

    @Override
    public void run(String... args) throws Exception {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("naver.local-search.client-id/secret are required for backfill");
        }

        List<Row> rows = jdbc.query(SELECT_SQL, (rs, rowNum) -> new Row(
            rs.getInt("store_id"),
            rs.getString("address"),
            rs.getString("place_id")
        ));

        int updated = 0;
        int skipped = 0;
        EnumMap<SkipReason, Integer> reasonCounts = new EnumMap<>(SkipReason.class);
        for (SkipReason reason : SkipReason.values()) {
            reasonCounts.put(reason, 0);
        }

        try (var writer = Files.newBufferedWriter(Path.of(CSV_PATH), StandardCharsets.UTF_8)) {
            writer.write("store_id,place_id,address,reason,detail");
            writer.newLine();

            for (Row row : rows) {
                if (row.address == null || row.address.isBlank()) {
                    skipped++;
                    increment(reasonCounts, SkipReason.NO_ADDRESS);
                    writeSkip(writer, row, SkipReason.NO_ADDRESS, "");
                    continue;
                }
                FetchResult result = fetchStoreName(row.address);
                if (result.name == null || result.name.isBlank()) {
                    skipped++;
                    increment(reasonCounts, result.reason);
                    writeSkip(writer, row, result.reason, result.detail);
                    continue;
                }
                String name = result.name;
                if (name.length() > MAX_NAME_LEN) {
                    name = name.substring(0, MAX_NAME_LEN);
                }

                MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("store_id", row.storeId)
                    .addValue("store_name", name);
                updated += jdbc.update(UPDATE_SQL, params);

                Thread.sleep(120);
            }
        }

        log.info("Naver backfill complete. total={}, updated={}, skipped={}", rows.size(), updated, skipped);
        log.info("Skip counts: {}", reasonCounts);
        log.info("Skip CSV written to {}", Path.of(CSV_PATH).toAbsolutePath());
        System.exit(0);
    }

    private FetchResult fetchStoreName(String address) {
        try {
            String encodedQuery = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://openapi.naver.com/v1/search/local.json?query="
                + encodedQuery + "&display=5&start=1&sort=comment";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return FetchResult.skip(SkipReason.HTTP_STATUS, String.valueOf(response.statusCode()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                return FetchResult.skip(SkipReason.NO_RESULTS, "");
            }

            String addressKey = normalizeAddress(address);
            for (JsonNode item : items) {
                String candidateAddress = item.path("roadAddress").asText("");
                if (candidateAddress.isBlank()) {
                    candidateAddress = item.path("address").asText("");
                }
                if (!candidateAddress.isBlank()) {
                    String candidateKey = normalizeAddress(candidateAddress);
                    if (!addressKey.isBlank() && candidateKey.contains(addressKey)) {
                        return FetchResult.ok(cleanTitle(item.path("title").asText("")));
                    }
                }
            }

            return FetchResult.ok(cleanTitle(items.get(0).path("title").asText("")));
        } catch (Exception e) {
            log.warn("Failed to fetch store name: {}", e.getMessage());
            return FetchResult.skip(SkipReason.EXCEPTION, e.getClass().getSimpleName());
        }
    }

    private String normalizeAddress(String address) {
        String normalized = address.replaceAll("\\s+", "");
        if (normalized.length() > 12) {
            normalized = normalized.substring(0, 12);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String cleanTitle(String title) {
        String cleaned = title.replaceAll("<[^>]+>", "");
        cleaned = cleaned.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
        return cleaned.trim();
    }

    private void writeSkip(java.io.BufferedWriter writer, Row row, SkipReason reason, String detail) throws Exception {
        writer.write(csvEscape(String.valueOf(row.storeId)));
        writer.write(",");
        writer.write(csvEscape(row.placeId));
        writer.write(",");
        writer.write(csvEscape(row.address));
        writer.write(",");
        writer.write(csvEscape(reason.name()));
        writer.write(",");
        writer.write(csvEscape(detail));
        writer.newLine();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private void increment(EnumMap<SkipReason, Integer> counts, SkipReason reason) {
        counts.put(reason, counts.getOrDefault(reason, 0) + 1);
    }

    private record Row(int storeId, String address, String placeId) {}

    private record FetchResult(String name, SkipReason reason, String detail) {
        static FetchResult ok(String name) {
            return new FetchResult(name, null, "");
        }

        static FetchResult skip(SkipReason reason, String detail) {
            return new FetchResult(null, reason, detail);
        }
    }

    private enum SkipReason {
        NO_ADDRESS,
        NO_RESULTS,
        HTTP_STATUS,
        EXCEPTION
    }
}
