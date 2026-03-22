package uk.co.asepstrath.bank.evaluation;

import java.util.Optional;

// gets market prices for a ticker (ASEP or test double)
public interface AsepPriceSource {

    // return latest positive price from the API - empty if missing or invalid
    Optional<AsepLatestPrice> latestPrice(String normalizedTicker);

    // last trading session EOD = second last valid date in ASEP
    default Optional<AsepLatestPrice> priorTradingDayEod(String normalizedTicker) {
        return Optional.empty();
    }
}
