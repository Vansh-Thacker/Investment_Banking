package uk.co.asepstrath.bank.evaluation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves equity metadata (e.g. stock name) by ticker.
 * Populated from GET /api/equities (Name, Ticker, Sector). When no record exists, returns ticker as fallback.
 */
public class EquitiesService {

    private final Map<String, EquityRecord> byTicker = new HashMap<>();

    public void addEquity(EquityRecord record) {
        if (record != null && record.ticker != null) {
            byTicker.put(record.ticker.trim().toUpperCase(), record);
        }
    }

    public EquityRecord getEquity(String ticker) {
        if (ticker == null || ticker.isBlank()) return null;
        return byTicker.get(ticker.trim().toUpperCase());
    }

    /** Returns display name for ticker, or ticker if not found (for transaction list). */
    public String getName(String ticker) {
        EquityRecord e = getEquity(ticker);
        if (e != null && e.name != null && !e.name.isBlank()) {
            return e.name;
        }
        return ticker != null ? ticker : "";
    }

    public Collection<EquityRecord> getAllEquities() {
        return byTicker.values();
    }
}
