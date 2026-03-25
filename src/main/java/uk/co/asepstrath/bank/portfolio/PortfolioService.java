package uk.co.asepstrath.bank.portfolio;

import java.util.*;
import kong.unirest.core.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

public class PortfolioService {

    private static final String BASE = "https://api.asep-strath.co.uk";

    public PortfolioService(String country, String region, String industry) {
        // kept for compatibility
    }

    // Fetch all equities from /api/equities
    public List<Map<String, Object>> getAllEquities() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            var response = Unirest.get(BASE + "/api/equities").asString();
            if (response.isSuccess()) {
                JSONObject body = new JSONObject(response.getBody());
                JSONArray assets = body.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject eq = assets.getJSONObject(i);
                        Map<String, Object> map = new HashMap<>();
                        map.put("ticker", eq.optString("ticker", "Unknown"));
                        map.put("name",   eq.optString("name",   "Unknown"));
                        map.put("sector", eq.optString("sector", "Unknown"));
                        // Fetch latest price for this equity
                        double price = getLatestPrice(eq.optString("ticker", ""));
                        map.put("price", price);
                        // We don't have shares from this endpoint - default 0
                        // This will be overridden when we join with transactions
                        map.put("shares", 0);
                        map.put("marketCap", price);
                        // No country/region from this API - mark unknown
                        map.put("country", "Unknown");
                        map.put("region",  "Unknown");
                        results.add(map);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    // Fetch latest price for a ticker from /api/prices/{symbol}
    public double getLatestPrice(String ticker) {
        try {
            var response = Unirest.get(BASE + "/api/prices/" + ticker).asString();
            if (response.isSuccess()) {
                JSONObject body = new JSONObject(response.getBody());
                JSONArray prices = body.optJSONArray("prices");
                if (prices != null && !prices.isEmpty()) {
                    // Last entry is most recent
                    JSONObject latest = prices.getJSONObject(prices.length() - 1);
                    return latest.optDouble("price", 0.0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // Fetch all transactions (handles pagination)
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

                // Handle pagination
                JSONObject pagination = body.optJSONObject("pagination");
                if (pagination != null && pagination.has("nextToken") && !pagination.isNull("nextToken")) {
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

    // Build breakdown (country/region/sector) with 5% "Other" rule
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

        for (Map.Entry<String, Double> entry : groups.entrySet()) {
            double percent = grandTotal > 0 ? (entry.getValue() / grandTotal) * 100 : 0;
            if (percent < 5.0) {
                otherValue += entry.getValue();
            } else {
                results.add(new PortfolioBreakdown(entry.getKey(), entry.getValue(), percent));
            }
        }

        if (otherValue > 0) {
            results.add(new PortfolioBreakdown("Other", otherValue, (otherValue / grandTotal) * 100));
        }

        results.sort((a, b) -> Double.compare(b.totalValue, a.totalValue));
        return results;
    }

    // Keep for compatibility
    public List<Map<String, Object>> getEquityData(List<String> tickers) {
        return getAllEquities();
    }
}