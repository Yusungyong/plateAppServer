package com.plateapp.plate_main.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

@Component
@Profile("backfill-store-name")
@RequiredArgsConstructor
@Slf4j
public class StoreNameBackfillRunner implements CommandLineRunner {

    private static final String SELECT_SQL = """
        SELECT store_id, place_id
        FROM fp_300
        WHERE place_id IS NOT NULL
          AND (store_name IS NULL OR store_name LIKE 'www%')
        """;

    private static final String UPDATE_SQL = """
        UPDATE fp_300
        SET store_name = :store_name
        WHERE store_id = :store_id
        """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Value("${google.places-api-key:}")
    private String apiKey;

    @Override
    public void run(String... args) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("google.places-api-key is required for backfill");
        }

        List<PlaceRow> rows = jdbc.query(SELECT_SQL, (rs, rowNum) -> new PlaceRow(
            rs.getInt("store_id"),
            rs.getString("place_id")
        ));

        int updated = 0;
        int skipped = 0;
        for (PlaceRow row : rows) {
            if (row.placeId == null || row.placeId.isBlank()) {
                skipped++;
                continue;
            }
            String name = fetchPlaceName(row.placeId);
            if (name == null || name.isBlank()) {
                skipped++;
                continue;
            }
            if (name.length() > 50) {
                name = name.substring(0, 50);
            }

            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("store_id", row.storeId)
                .addValue("store_name", name);
            updated += jdbc.update(UPDATE_SQL, params);

            Thread.sleep(120); // small throttle to avoid rate limits
        }

        log.info("Backfill complete. total={}, updated={}, skipped={}", rows.size(), updated, skipped);
        System.exit(0);
    }

    private String fetchPlaceName(String placeId) {
        try {
            String encodedPlaceId = URLEncoder.encode(placeId, StandardCharsets.UTF_8);
            String url = "https://maps.googleapis.com/maps/api/place/details/json?place_id="
                + encodedPlaceId + "&fields=name&key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                return null;
            }
            return root.path("result").path("name").asText();
        } catch (Exception e) {
            log.warn("Failed to fetch place name: {}", e.getMessage());
            return null;
        }
    }

    private record PlaceRow(int storeId, String placeId) {}
}
