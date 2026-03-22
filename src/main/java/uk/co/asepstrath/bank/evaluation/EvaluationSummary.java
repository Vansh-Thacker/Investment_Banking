package uk.co.asepstrath.bank.evaluation;

import java.util.ArrayList;
import java.util.List;

public class EvaluationSummary {
    public String sourceNote;
    public String priceSourceNote;
    public int positionCount;

    public long totalBookValue;
    public String totalBookValueGbp;

    public double totalMarketValue;
    public String totalMarketValueGbp;
    public int positionsWithMarketPriceCount;
    public int positionsWithoutMarketPriceCount;

    public int positionsWithPriorEodCount;
    public int positionsWithoutPriorEodCount;

    public double totalValueAtPriorClose;
    public String totalValueAtPriorCloseGbp;
    public int positionsWithPriceCount;
    public int positionsWithoutPriceCount;

    public int cashBalance;
    public String cashBalanceGbp;
    public double positionsMarketValue;
    public String positionsMarketValueGbp;
    public double totalPortfolioValue;
    public String totalPortfolioValueGbp;
    public long totalBookValueAllPositions;
    public String totalBookValueAllPositionsGbp;
    public Double totalUnrealizedPnL;
    public String totalUnrealizedPnLGbp;
    public Double totalUnrealizedPnLPercent;
    public String totalUnrealizedPnLPercentDisplay;

    public List<String> notices = new ArrayList<>();
}
