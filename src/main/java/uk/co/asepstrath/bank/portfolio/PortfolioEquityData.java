package uk.co.asepstrath.bank.portfolio;

import io.jooby.Context;
import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioEquityData {
    @GET("/portfolioEquity")
    public Map<String, Object> getPortfolioEquity(@QueryParam("tickers") String tickers, Context ctx) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        String username = ctx.session().get("user").value((String) null);
        if (username == null) {
            ctx.sendRedirect("/login");
            return null;
        }

        System.out.println(username);

        if (tickers == null || tickers.isEmpty()) {
            throw new IllegalArgumentException("Tickers parameter is required");
        }

        String[] tickerList = tickers.split(",");


        result.put("data", data);
        System.out.println(result);
        return result;
    }

}
