package uk.co.asepstrath.bank.portfolio;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import kong.unirest.core.HttpResponse; // Use this instead of java.net.http
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.*;


public class PortfolioService {
    private final String Country;
    private final String Region;
    private final String Industry;
    public PortfolioService(String Country, String Region, String Industry) {
        this.Country = Country;
        this.Region = Region;
        this.Industry = Industry;
    }
    public String getCountry() {
        return Country;
    }
    public String getRegion() {
        return Region;
    }
    public String getIndustry() {
        return Industry;
    }


    private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();
    private final int maxReqs = 100;
    private final long reqTime = 60_000L;

    public List<Map<String, Object>>  getEquityData(List<String> tickers){
        List<Map<String, Object>>  resultList = new ArrayList<>();

        for (String ticker : tickers) {
            ticker = ticker.trim().toLowerCase();
            try{
                reqsLimit();

                String encodedTicker = java.net.URLEncoder.encode(
                        ticker,
                        StandardCharsets.UTF_8
                );
                String url = "https://api.portfolio.com/v1/ticker/" + encodedTicker;

                HttpResponse<JsonNode> response = Unirest.get(url).asJson();
                JSONObject json = response.getBody().getObject();
                JSONArray data = json.optJSONArray("data");

                if (data != null && data.length() > 0) {
                    JSONObject equity = data.getJSONObject(0);


                    String country = equity.optString("country", "Unknown");
                    equity.put("region", RegionMapper.Regionmap(country));


                    Map<String, Object> map = new HashMap<>();
                    for (String key : equity.keySet()) {
                        map.put(key, equity.get(key));
                    }

                    resultList.add(map);
                }
            } catch (Exception e){
               resultList.add(buildUnknown(ticker));
            }
        }
        return resultList;
    }

    private void reqsLimit() throws InterruptedException {
        long now = System.currentTimeMillis();
        while (!timestamps.isEmpty() && now - timestamps.peek() > reqTime) {
            timestamps.poll();
        }
        if (timestamps.size() >= maxReqs) {
            long wait = reqTime - (now - timestamps.peek());
            if (wait > 0) Thread.sleep(wait);
        }
        timestamps.add(System.currentTimeMillis());
    }

    private Map<String, Object> buildUnknown(String ticker) {
        Map<String, Object> unknown = new HashMap<>();
        unknown.put("ticker", ticker);
        unknown.put("name", "Unknown");
        unknown.put("country", "Unknown");
        unknown.put("region", "Unknown");
        unknown.put("sector", "Unknown");
        unknown.put("marketCap", 0);
        return unknown;
    }








}
