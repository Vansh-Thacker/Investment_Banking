package uk.co.asepstrath.bank.portfolio;

import java.util.*;
import kong.unirest.core.Unirest;

public class PortfolioService {
    private final String country, region, industry;
    private int reqCount = 0;
    private long startTime = System.currentTimeMillis();

    public PortfolioService(String country, String region, String industry) {
        this.country = country;
        this.region = region;
        this.industry = industry;
    }

    public List<Map<String, Object>> getEquityData(List<String> tickers) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String ticker : tickers) {
            rateLimit();
            try {
                var response = Unirest.get("https://api.asep-strath.co.uk/api/equities/metadata/" + ticker.trim()).asJson();

                if (response.isSuccess() && !response.getBody().getArray().isEmpty()) {
                    var data = response.getBody().getArray().getJSONObject(0);

                    String c = data.optString("country", "Unknown");
                    data.put("region", RegionMapper.Regionmap(c));

                    results.add(data.toMap());
                } else {
                    results.add(buildUnknown(ticker));
                }
            } catch (Exception e) {
                results.add(buildUnknown(ticker));
            }
        }
        return results;
    }

    private void rateLimit() {
        if (++reqCount >= 100) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < 60000) {
                try { Thread.sleep(60000 - elapsed); } catch (Exception ignored) {}
            }
            reqCount = 0;
            startTime = System.currentTimeMillis();
        }
    }

    private Map<String, Object> buildUnknown(String t) {
        Map<String, Object> map = new HashMap<>();
        map.put("ticker", t);
        map.put("name", "Unknown");
        map.put("marketCap", 0);
        return map;
    }
}