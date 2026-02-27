package uk.co.asepstrath.bank.investments;

import io.jooby.annotation.*;
import org.slf4j.Logger;
import uk.co.asepstrath.bank.Account;

import javax.sql.DataSource;
import java.util.List;

@Path("/investments")
public class InvestmentController {

    private static final String FILTER_PURCHASES = "purchases";

    private final InvestmentService service;
    private final Logger log;

    public InvestmentController(DataSource ds, Logger log, Account account) {
        this.service = new InvestmentService(account);
        this.log = log;
    }

    @GET("/positions")
    public Object positions() {
        return service.getPositions();
    }

    
    // stock history. filter=purchases see only buy transactions sorted by timestamp
     
    @GET("/transactions")
    public Object transactions(@QueryParam String filter, @QueryParam String sort) {
        if (FILTER_PURCHASES.equalsIgnoreCase(filter)) {
            return service.getPurchaseHistory(sort);
        }
        return service.getHistory();
    }

    
     // preview sale details for the confirmation screen (symbol, name, shares, est price and proceeds)
     // no sale executed, user must call post /sell to confirm and execute
     
    @POST("/sell/preview")
    public Object sellPreview(TradeRequest request) {
        return service.getSalePreview(request.ticker, request.shares, request.price);
    }

    
    // execute sale. User should have reviewed details via psot /sell/preview first. Returns confirmation
    
    @POST("/sell")
    public Object sell(TradeRequest request) {
        return service.sell(request.ticker, request.shares, request.price);
    }
}
