package uk.co.asepstrath.bank.evaluation;

public class EvaluationPositionLine {
    public String ticker;
    public String displayName;
    public String sector;
    public int shares;

    public int bookPricePerShare;
    public String bookPricePerShareGbp;
    public int averageBookPricePerShare;
    public String averageBookPricePerShareGbp;
    public int totalInvested;
    public String totalInvestedGbp;
    public long bookValue;
    public String bookValueGbp;

    public boolean referenceMarketPriceAvailable;
    public Double referenceMarketPricePerShare;
    public String referenceMarketPricePerShareGbp;
    public String referencePriceAsOfDate;

    public boolean marketPriceAvailable;
    public Double marketPricePerShare;
    public String marketPricePerShareGbp;
    public String marketPriceAsOfDate;
    public Double latestClosePricePerShare;
    public String latestClosePricePerShareGbp;
    public String priceAsOfDate;
    public Double currentMarketValue;
    public String currentMarketValueGbp;

    public boolean priorEodAvailable;
    public Double priorEodPricePerShare;
    public String priorEodPricePerShareGbp;

    public boolean priorEodPriceAvailable;
    public Double priorEodClosePerShare;
    public String priorEodClosePerShareGbp;
    public String priorSessionDate;
    public Double valueAtPriorClose;
    public String valueAtPriorCloseGbp;

    public Double unrealizedPnL;
    public String unrealizedPnLGbp;
    public Double unrealizedPnLPercent;
    public String unrealizedPnLPercentDisplay;

    public String statusLabel;
    public String glanceLine;
}
