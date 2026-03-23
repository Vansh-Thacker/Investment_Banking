package uk.co.asepstrath.bank.evaluation;

public class CurrentPriceService {

    private final CurrentPriceLoader loader;

    public CurrentPriceService(CurrentPriceLoader loader) {
        this.loader = loader;
    }

    public int getPricePence(String ticker) {
        Integer p = loader.getCurrentPrice(ticker);
        return p != null ? p : 0;
    }
}
