package uk.co.asepstrath.bank.portfolio;

public class PortfolioService {
    private final String Country;
    private final String Region;
    private final String Industry;
    public PortfolioService(String Country, String Region, String Industry) {
        this.Country = Country;
        this.Region = Region;
        this.Industry = Industry;
    }
    public String getCountry() {
        return Country;
    }
    public String getRegion() {
        return Region;
    }
    public String getIndustry() {
        return Industry;
    }





}
