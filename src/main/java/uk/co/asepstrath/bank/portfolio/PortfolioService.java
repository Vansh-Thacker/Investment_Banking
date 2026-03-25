package uk.co.asepstrath.bank.portfolio;

import java.util.*;
import kong.unirest.core.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

public class PortfolioService {

    private static final String BASE = "https://api.asep-strath.co.uk";

    public PortfolioService(String country, String region, String industry) {}

    public List<Map<String, Object>> getAllEquities() {
        // Step 1: Get all tickers
        List<String> tickers = new ArrayList<>();
        try {
            var response = Unirest.get(BASE + "/api/equities").asString();
            if (response.isSuccess()) {
                JSONObject body = new JSONObject(response.getBody());
                JSONArray assets = body.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        String ticker = assets.getJSONObject(i).optString("ticker", "");
                        if (!ticker.isEmpty()) tickers.add(ticker);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tickers.isEmpty()) return new ArrayList<>();

        // Step 2: Batch metadata call — response uses "assets" not "data"
        String tickerParam = String.join(",", tickers);
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            var response = Unirest.get(BASE + "/api/equities/metadata")
                    .queryString("tickers", tickerParam)
                    .asString();

            if (response.isSuccess()) {
                JSONObject body = new JSONObject(response.getBody());

                // FIX: API returns "assets" not "data"
                JSONArray assets = body.optJSONArray("assets");

                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject eq = assets.getJSONObject(i);

                        String country = eq.optString("country", "Unknown");
                        String region  = eq.optString("region",  "Unknown");
                        if (region.isEmpty() || region.equals("Unknown")) {
                            region = RegionMapper.Regionmap(country);
                        }

                        Map<String, Object> map = new HashMap<>();
                        map.put("ticker",    eq.optString("ticker",  "Unknown"));
                        map.put("name",      eq.optString("name",    "Unknown"));
                        map.put("sector",    eq.optString("sector",  "Unknown"));
                        map.put("country",   country);
                        map.put("region",    region);
                        // FIX: use marketCap directly from metadata, don't fetch prices
                        map.put("marketCap", eq.optDouble("marketCap", 0.0));

                        results.add(map);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }



    public List<Map<String, Object>> getAllTransactions() {
        List<Map<String, Object>> all = new ArrayList<>();
        String url = BASE + "/api/transactions";

        while (url != null) {
            try {
                var response = Unirest.get(url).asString();
                if (!response.isSuccess()) break;

                JSONObject body = new JSONObject(response.getBody());
                JSONArray txs = body.optJSONArray("transactions");

                if (txs != null) {
                    for (int i = 0; i < txs.length(); i++) {
                        JSONObject tx = txs.getJSONObject(i);
                        Map<String, Object> map = new HashMap<>();
                        map.put("id",              tx.optString("id"));
                        map.put("date",            tx.optString("date"));
                        map.put("type",            tx.optString("type"));
                        map.put("ticker",          tx.optString("ticker", ""));
                        map.put("investorId",      tx.optString("investorId"));
                        map.put("totalCashAmount", tx.optDouble("totalCashAmount", 0));
                        all.add(map);
                    }
                }

                JSONObject pagination = body.optJSONObject("pagination");
                if (pagination != null && !pagination.isNull("nextToken")) {
                    String token = pagination.optString("nextToken", "");
                    url = token.isEmpty() ? null : BASE + "/api/transactions?nextToken=" + token;
                } else {
                    url = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return all;
    }

    public List<PortfolioBreakdown> getBreakdown(List<Map<String, Object>> equities, String type) {
        Map<String, Double> groups = new HashMap<>();
        double grandTotal = 0;

        for (Map<String, Object> eq : equities) {
            double value = Double.parseDouble(eq.getOrDefault("marketCap", 0).toString());
            String key = eq.getOrDefault(type, "Unknown").toString();
            groups.put(key, groups.getOrDefault(key, 0.0) + value);
            grandTotal += value;
        }

        List<PortfolioBreakdown> results = new ArrayList<>();
        double otherValue = 0;
        final double total = grandTotal;

        for (Map.Entry<String, Double> entry : groups.entrySet()) {
            double percent = total > 0 ? (entry.getValue() / total) * 100 : 0;
            if (percent < 5.0) {
                otherValue += entry.getValue();
            } else {
                results.add(new PortfolioBreakdown(entry.getKey(), entry.getValue(), percent));
            }
        }

        if (otherValue > 0) {
            results.add(new PortfolioBreakdown("Other", otherValue, (otherValue / total) * 100));
        }

        results.sort((a, b) -> Double.compare(b.totalValue, a.totalValue));
        return results;
    }

    // Kept for compatibility
    public List<Map<String, Object>> getEquityData(List<String> tickers) {
        return getAllEquities();
    }
}