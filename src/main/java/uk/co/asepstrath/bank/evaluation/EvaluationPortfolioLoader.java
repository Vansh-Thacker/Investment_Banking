package uk.co.asepstrath.bank.evaluation;

import uk.co.asepstrath.bank.Account;
import uk.co.asepstrath.bank.investments.InvestmentService;
import uk.co.asepstrath.bank.user.User;
import uk.co.asepstrath.bank.user.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// loads per-user portfolio data for eval (not final — Mockito subclasses this in tests)
public class EvaluationPortfolioLoader {

    // NEW: current price loader + service for currentprices.csv
    CurrentPriceLoader currentPriceLoader = new CurrentPriceLoader("currentprices.csv");
    CurrentPriceService currentPriceService = new CurrentPriceService(currentPriceLoader);

    private final UserService users;

    public EvaluationPortfolioLoader(UserService users) {
        this.users = users;
    }

    public InvestmentService loadForUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId required");
        }
        User user = users.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user");
        }
        Account account = new Account();
        account.forceSetBalance(user.getBalancePence());
        InvestmentService loaded = new InvestmentService(account, currentPriceLoader);
        PriceLoader priceLoader = new PriceLoader("prices.csv");
        Path p = Paths.get("UserStats/" + userId + "/history.csv");
        try {
            if (Files.exists(p)) {
                loaded.importCsv(Files.newInputStream(p), priceLoader);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load investment CSV for user " + userId, e);
        }
        return loaded;
    }
}
