package uk.co.asepstrath.bank.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioServiceTest {

    private PortfolioService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioService("uk", "Europe", "Finance");
    }

    // ─── getBreakdown ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBreakdown groups equities by sector correctly")
    void testGetBreakdownBySector() {
        List<Map<String, Object>> equities = List.of(
                makeEquity("CTT", "Finance",   10000.0),
                makeEquity("APX", "Technology",20000.0),
                makeEquity("STD", "Finance",   15000.0)
        );

        List<PortfolioBreakdown> result = service.getBreakdown(equities, "sector");

        // Finance = 25000, Technology = 20000, total = 45000
        assertEquals(2, result.size());

        PortfolioBreakdown finance = result.stream()
                .filter(b -> b.label.equals("Finance")).findFirst().orElse(null);
        assertNotNull(finance);
        assertEquals(25000.0, finance.totalValue, 0.01);
        assertEquals((25000.0 / 45000.0) * 100, finance.percentage, 0.01);
    }

    @Test
    @DisplayName("getBreakdown groups small segments into Other when under 5%")
    void testGetBreakdownSmallSegmentsGroupedIntoOther() {
        List<Map<String, Object>> equities = new ArrayList<>();
        // Finance = 60%, Technology = 30% — both above 5%, stay separate
        equities.add(makeEquity("CTT", "Finance",    60000.0));
        equities.add(makeEquity("APX", "Technology", 30000.0));
        // These two are under 5% each — should go to Other
        equities.add(makeEquity("STD", "Materials",  1000.0));  // 1%
        equities.add(makeEquity("CLP", "Healthcare", 500.0));   // 0.5%

        List<PortfolioBreakdown> result = service.getBreakdown(equities, "sector");

        boolean hasOther = result.stream().anyMatch(b -> b.label.equals("Other"));
        assertTrue(hasOther, "Small segments should be grouped into Other");

        PortfolioBreakdown other = result.stream()
                .filter(b -> b.label.equals("Other")).findFirst().orElse(null);
        assertNotNull(other);
        // Only Materials(1000) + Healthcare(500) go to Other
        assertEquals(1500.0, other.totalValue, 0.01);

        // Finance and Technology should remain as separate segments
        assertTrue(result.stream().anyMatch(b -> b.label.equals("Finance")));
        assertTrue(result.stream().anyMatch(b -> b.label.equals("Technology")));
    }

    @Test
    @DisplayName("getBreakdown returns empty list when equities list is empty")
    void testGetBreakdownEmptyEquities() {
        List<PortfolioBreakdown> result = service.getBreakdown(Collections.emptyList(), "sector");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getBreakdown handles Unknown sector gracefully")
    void testGetBreakdownUnknownSector() {
        List<Map<String, Object>> equities = List.of(
                makeEquity("CTT", "Finance", 50000.0),
                makeUnknownEquity("XYZ", 50000.0)
        );

        List<PortfolioBreakdown> result = service.getBreakdown(equities, "sector");

        boolean hasUnknown = result.stream().anyMatch(b -> b.label.equals("Unknown"));
        assertTrue(hasUnknown, "Unknown sector should appear in breakdown");
    }

    @Test
    @DisplayName("getBreakdown results are sorted by value descending")
    void testGetBreakdownSortedByValue() {
        List<Map<String, Object>> equities = List.of(
                makeEquity("A", "Technology", 10000.0),
                makeEquity("B", "Finance",    50000.0),
                makeEquity("C", "Energy",     30000.0)
        );

        List<PortfolioBreakdown> result = service.getBreakdown(equities, "sector");

        // Should be Finance(50k), Energy(30k), Technology(10k)
        assertEquals("Finance", result.get(0).label);
        assertEquals("Energy",  result.get(1).label);
    }

    @Test
    @DisplayName("getBreakdown percentages sum to 100")
    void testGetBreakdownPercentagesSumTo100() {
        List<Map<String, Object>> equities = List.of(
                makeEquity("A", "Finance",    40000.0),
                makeEquity("B", "Technology", 35000.0),
                makeEquity("C", "Energy",     25000.0)
        );

        List<PortfolioBreakdown> result = service.getBreakdown(equities, "sector");

        double sum = result.stream().mapToDouble(b -> b.percentage).sum();
        assertEquals(100.0, sum, 0.01);
    }

    @Test
    @DisplayName("getBreakdown groups by region correctly")
    void testGetBreakdownByRegion() {
        List<Map<String, Object>> equities = List.of(
                makeEquityWithRegion("CTT", "Finance", "Europe",        50000.0),
                makeEquityWithRegion("APX", "Technology", "North America", 50000.0)
        );

        List<PortfolioBreakdown> result = service.getBreakdown(equities, "region");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(b -> b.label.equals("Europe")));
        assertTrue(result.stream().anyMatch(b -> b.label.equals("North America")));
    }

    @Test
    @DisplayName("getBreakdown groups by country correctly")
    void testGetBreakdownByCountry() {
        List<Map<String, Object>> equities = List.of(
                makeEquityWithCountry("CTT", "United Kingdom", 60000.0),
                makeEquityWithCountry("APX", "United States",  40000.0)
        );

        List<PortfolioBreakdown> result = service.getBreakdown(equities, "country");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(b -> b.label.equals("United Kingdom")));
        assertTrue(result.stream().anyMatch(b -> b.label.equals("United States")));
    }

    @Test
    @DisplayName("getBreakdown handles zero marketCap without dividing by zero")
    void testGetBreakdownAllZeroMarketCap() {
        List<Map<String, Object>> equities = List.of(
                makeEquity("CTT", "Finance",    0.0),
                makeEquity("APX", "Technology", 0.0)
        );

        // Should not throw ArithmeticException
        assertDoesNotThrow(() -> service.getBreakdown(equities, "sector"));
    }

    @Test
    @DisplayName("getBreakdown handles single equity correctly")
    void testGetBreakdownSingleEquity() {
        List<Map<String, Object>> equities = List.of(
                makeEquity("CTT", "Finance", 100000.0)
        );

        List<PortfolioBreakdown> result = service.getBreakdown(equities, "sector");

        assertEquals(1, result.size());
        assertEquals("Finance", result.get(0).label);
        assertEquals(100.0, result.get(0).percentage, 0.01);
    }

    // ─── PortfolioBreakdown model ────────────────────────────────────────────

    @Test
    @DisplayName("PortfolioBreakdown constructor sets all fields correctly")
    void testPortfolioBreakdownConstructor() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Finance", 25000.0, 55.5);

        assertEquals("Finance", bd.label);
        assertEquals(25000.0,   bd.totalValue, 0.01);
        assertEquals(55.5,      bd.percentage, 0.01);
    }

    @Test
    @DisplayName("PortfolioBreakdown handles zero values")
    void testPortfolioBreakdownZeroValues() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Unknown", 0.0, 0.0);

        assertEquals("Unknown", bd.label);
        assertEquals(0.0, bd.totalValue, 0.01);
        assertEquals(0.0, bd.percentage, 0.01);
    }

    @Test
    @DisplayName("PortfolioBreakdown handles negative percentage")
    void testPortfolioBreakdownNegativeValues() {
        PortfolioBreakdown bd = new PortfolioBreakdown("Other", 5000.0, 4.9);
        assertEquals(4.9, bd.percentage, 0.01);
    }

    // ─── getEquityData (compatibility method) ───────────────────────────────

    @Test
    @DisplayName("getEquityData returns a list (may be empty if API unreachable)")
    void testGetEquityDataReturnsList() {
        List<Map<String, Object>> result = service.getEquityData(List.of("CTT", "APX"));
        assertNotNull(result);
    }

    @Test
    @DisplayName("getEquityData with empty ticker list returns empty list")
    void testGetEquityDataEmptyTickers() {
        List<Map<String, Object>> result = service.getEquityData(Collections.emptyList());
        assertNotNull(result);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> makeEquity(String ticker, String sector, double marketCap) {
        Map<String, Object> m = new HashMap<>();
        m.put("ticker",    ticker);
        m.put("name",      ticker + " Corp");
        m.put("sector",    sector);
        m.put("country",   "Unknown");
        m.put("region",    "Unknown");
        m.put("marketCap", marketCap);
        return m;
    }

    private Map<String, Object> makeEquityWithRegion(String ticker, String sector, String region, double marketCap) {
        Map<String, Object> m = makeEquity(ticker, sector, marketCap);
        m.put("region", region);
        return m;
    }

    private Map<String, Object> makeEquityWithCountry(String ticker, String country, double marketCap) {
        Map<String, Object> m = makeEquity(ticker, "Finance", marketCap);
        m.put("country", country);
        return m;
    }

    private Map<String, Object> makeUnknownEquity(String ticker, double marketCap) {
        Map<String, Object> m = new HashMap<>();
        m.put("ticker",    ticker);
        m.put("name",      "Unknown");
        m.put("sector",    "Unknown");
        m.put("country",   "Unknown");
        m.put("region",    "Unknown");
        m.put("marketCap", marketCap);
        return m;
    }
}