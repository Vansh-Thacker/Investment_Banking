package uk.co.asepstrath.bank.evaluation;
import uk.co.asepstrath.bank.investments.InvestmentService;
import uk.co.asepstrath.bank.investments.Position;

public class EvaluationService {

    private final InvestmentService investmentService;
    private final PriceService priceService;

    public EvaluationService(InvestmentService investmentService, PriceService priceService) {
        this.investmentService = investmentService;
        this.priceService = priceService;
    }

    // get the total current value of all stocks
    public double getPortfolioValue() {
        double total = 0;
        
        // loop through every stock position
        for (Position position : investmentService.getPositions()) {
            // get the current price the specific stock stock
            PriceRecord price = priceService.getPrice(position.ticker);
            
            // only add price if there is one
            if (price != null) {
                total = total + (position.shares * price.price);
            }
        }
        
        return total;
    }

    // get how much money we made or lost by calculating difference current value, and invested value
    public double getStockStatus() {
        double currentValue = getPortfolioValue();
        double amountInvested = getTotalInvested();
        double profitOrLoss = currentValue - amountInvested;
        return profitOrLoss;
    }

    // get the percentage return (gain or loss)
    public double getReturnPercentage() {
        double amountInvested = getTotalInvested();
        
        // if we haven't invested anything, return 0%
        if (amountInvested == 0) {
            return 0;
        }
        
        double profit = getStockStatus();
        double percentage = (profit / amountInvested) * 100;
        return percentage;
    }

    // method to help add up all the money we spent on stocks
    private double getTotalInvested() {
        double total = 0;
        
        for (Position position : investmentService.getPositions()) {
            total = total + position.totalInvested;
        }
        
        return total;
    }

}