package uk.co.asepstrath.bank.portfolio;

import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;
import java.net.http.HttpClient;
import java.util.*;

public class PortfolioEquityData {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final Queue<Long> timestamps = new LinkedList<>();
    private final int maxReqs = 100;
    private final long time = 60_000;

    private final PortfolioService service = new PortfolioService("uk", "Europe", "Finance");

    @GET("/portfolioEquity")
    public Map<String, Object> getPortfolioEquity(
            @QueryParam("tickers") String tickers, Context ctx) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        String username = ctx.session().get("user").value((String) null);
        if (username == null) {
            ctx.sendRedirect("/login");
            return null;
        }
        if (tickers == null || tickers.isEmpty()) {
            throw new IllegalArgumentException("Tickers parameter is required");
        }

        List<String> tickersList = Arrays.asList(tickers.split(","));
        List<Map<String,Object>> equityData = service.getEquityData(tickersList);


        result.put("data", data);
        return result;
    }

}
