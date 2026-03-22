package uk.co.asepstrath.bank.evaluation;

import java.util.ArrayList;
import java.util.List;

public class EvaluationSectorGroup {
    public String sectorKey;
    public String sectorLabel;
    public List<String> tickers = new ArrayList<>();
    public long sectorBookValue;
    public String sectorBookValueGbp;
    public double sectorMarketValue;
    public String sectorMarketValueGbp;
    public double sectorUnrealizedPnL;
    public String sectorUnrealizedPnLGbp;
    public double sectorValueAtPriorClose;
    public String sectorValueAtPriorCloseGbp;
    public int positionCount;
}
