package uk.co.asepstrath.bank.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// gets prices from ASEP {@code GET /api/prices/{symbol}} and returns the latest available point (by date)
public class AsepApiClient implements AsepPriceSource {

    private static final Logger log = LoggerFactory.getLogger(AsepApiClient.class);

    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public AsepApiClient(String baseUrl) {
        this(baseUrl, new ObjectMapper());
    }

    public AsepApiClient(String baseUrl, ObjectMapper objectMapper) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("asep baseUrl is required");
        }
        String trimmed = baseUrl.trim();
        this.baseUrl = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AsepLatestPrice> latestPrice(String normalizedTicker) {
        if (normalizedTicker == null || normalizedTicker.isBlank()) {
            return Optional.empty();
        }
        String enc = URLEncoder.encode(normalizedTicker.trim(), StandardCharsets.UTF_8);
        String url = baseUrl + "/api/prices/" + enc;
        try {
            HttpResponse<String> response = Unirest.get(url).asString();
            if (!response.isSuccess() || response.getBody() == null || response.getBody().isBlank()) {
                log.debug("ASEP prices request failed for {}: status {}", normalizedTicker, response.getStatus());
                return Optional.empty();
            }
            return parseLatest(response.getBody());
        } catch (Exception e) {
            log.debug("ASEP prices request error for {}: {}", normalizedTicker, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<AsepLatestPrice> priorTradingDayEod(String normalizedTicker) {
        if (normalizedTicker == null || normalizedTicker.isBlank()) {
            return Optional.empty();
        }
        String enc = URLEncoder.encode(normalizedTicker.trim(), StandardCharsets.UTF_8);
        String url = baseUrl + "/api/prices/" + enc;
        try {
            HttpResponse<String> response = Unirest.get(url).asString();
            if (!response.isSuccess() || response.getBody() == null || response.getBody().isBlank()) {
                log.debug("ASEP prices request failed for prior EOD {}: status {}", normalizedTicker, response.getStatus());
                return Optional.empty();
            }
            return parsePriorTradingDayEod(response.getBody());
        } catch (Exception e) {
            log.debug("ASEP prices request error for prior EOD {}: {}", normalizedTicker, e.getMessage());
            return Optional.empty();
        }
    }

    Optional<AsepLatestPrice> parseLatest(String jsonBody) {
        List<AsepPriceEntry> valid = parseValidSortedEntries(jsonBody);
        if (valid.isEmpty()) {
            return Optional.empty();
        }
        AsepPriceEntry last = valid.get(valid.size() - 1);
        return Optional.of(toLatest(last));
    }

    Optional<AsepLatestPrice> parsePriorTradingDayEod(String jsonBody) {
        List<AsepPriceEntry> valid = parseValidSortedEntries(jsonBody);
        if (valid.size() < 2) {
            return Optional.empty();
        }
        AsepPriceEntry prior = valid.get(valid.size() - 2);
        return Optional.of(toLatest(prior));
    }

    private static AsepLatestPrice toLatest(AsepPriceEntry e) {
        AsepLatestPrice out = new AsepLatestPrice();
        out.pricePerShare = e.price;
        out.date = e.date.trim();
        return out;
    }

    private List<AsepPriceEntry> parseValidSortedEntries(String jsonBody) {
        try {
            AsepPricesEnvelope env = objectMapper.readValue(jsonBody, AsepPricesEnvelope.class);
            if (env.prices == null || env.prices.isEmpty()) {
                return List.of();
            }
            List<AsepPriceEntry> valid = new ArrayList<>();
            for (AsepPriceEntry e : env.prices) {
                if (e == null || e.date == null || e.date.isBlank()) {
                    continue;
                }
                if (e.price <= 0 || Double.isNaN(e.price) || Double.isInfinite(e.price)) {
                    continue;
                }
                try {
                    LocalDate.parse(e.date.trim());
                } catch (DateTimeParseException ex) {
                    continue;
                }
                valid.add(e);
            }
            if (valid.isEmpty()) {
                return List.of();
            }
            valid.sort(Comparator.comparing(a -> LocalDate.parse(a.date.trim())));
            return valid;
        } catch (Exception e) {
            log.debug("Failed to parse ASEP prices JSON: {}", e.getMessage());
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AsepPricesEnvelope {
        public List<AsepPriceEntry> prices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AsepPriceEntry {
        public String date;
        public double price;
    }
}
