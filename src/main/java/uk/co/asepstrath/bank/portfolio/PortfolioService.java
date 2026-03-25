package uk.co.asepstrath.bank.portfolio;

import java.util.*;
import kong.unirest.core.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

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
                // asString() instead of asJson() - no JSON engine needed
                var response = Unirest.get("https://api.asep-strath.co.uk/api/equities/metadata/" + ticker.trim())
                        .asString();

                if (response.isSuccess()) {
                    JSONArray data = new JSONArray(response.getBody());
                    if (!data.isEmpty()) {
                        JSONObject equity = data.getJSONObject(0);
                        String c = equity.optString("country", "Unknown");
                        equity.put("region", RegionMapper.Regionmap(c));
                        results.add(equity.toMap());
                    } else {
                        results.add(buildUnknown(ticker));
                    }
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
        map.put("country", "Unknown");
        map.put("region", "Unknown");
        map.put("sector", "Unknown");
        map.put("marketCap", 0);
        return map;
    }

    public List<PortfolioBreakdown> getBreakdown(List<Map<String, Object>> equities, String type) {
        Map<String, Double> groups = new HashMap<>();
        double grandTotal = 0;

        for (Map<String, Object> eq : equities) {
            double value = Double.parseDouble(eq.getOrDefault("marketCap", 0).toString());
            String key = (String) eq.getOrDefault(type, "Unknown");
            groups.put(key, groups.getOrDefault(key, 0.0) + value);
            grandTotal += value;
        }

        List<PortfolioBreakdown> results = new ArrayList<>();
        double otherValue = 0;

        for (Map.Entry<String, Double> entry : groups.entrySet()) {
            double percent = (entry.getValue() / grandTotal) * 100;
            if (percent < 5.0) {
                otherValue += entry.getValue();
            } else {
                results.add(new PortfolioBreakdown(entry.getKey(), entry.getValue(), percent));
            }
        }

        if (otherValue > 0) {
            results.add(new PortfolioBreakdown("Other", otherValue, (otherValue / grandTotal) * 100));
        }

        return results;
    }
}