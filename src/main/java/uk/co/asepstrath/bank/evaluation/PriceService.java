package uk.co.asepstrath.bank.evaluation;

import java.util.*;

public class PriceService {

    private final Map<String, PriceRecord> prices = new HashMap<>();

    public void addPrice(PriceRecord record) {
        prices.put(record.ticker, record);
    }

    public PriceRecord getPrice(String ticker) {
        return prices.get(ticker);
    }

    public Collection<PriceRecord> getAllPrices() {
        return prices.values();
    }
}

