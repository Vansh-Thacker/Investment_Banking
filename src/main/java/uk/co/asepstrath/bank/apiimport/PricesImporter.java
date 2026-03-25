package uk.co.asepstrath.bank.apiimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.*;
import java.util.List;

public class PricesImporter {

    private static final String ROOT = "https://api.asep-strath.co.uk";

    // Hard-coded tickers because /api/stocks does not exist
    private static final List<String> TICKERS = List.of(
            "APX",
            "CRC",
            "CSCU",
            "FINX",
            "FSC",
            "GMX150",
            "HLN",
            "HZB",
            "MEDX",
            "MOG",
            "MRH",
            "NXP",
            "PHX",
            "PNTC",
            "QSY",
            "STRF",
            "TECHX",
            "TTN",
            "VGM",
            "ZNC"
    );

    public static void importPrices() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Write to currentprices.csv
        Path pricesCsv = Path.of("currentprices.csv");

        // ✅ Always clear and reset the CSV with header
        Files.writeString(
                pricesCsv,
                "ticker,pricePence\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        for (String ticker : TICKERS) {
            try {
                String url = ROOT + "/api/prices/" + ticker;
                System.out.println("Fetching: " + url);

                String json = ApiReader.get(url);
                JsonNode root = mapper.readTree(json);

                JsonNode pricesArray = root.get("prices");

                if (pricesArray == null || !pricesArray.isArray() || pricesArray.size() == 0) {
                    System.err.println("No prices returned for " + ticker);
                    continue;
                }

                // Latest price = last entry
                JsonNode latest = pricesArray.get(pricesArray.size() - 1);

                if (!latest.has("price")) {
                    System.err.println("Missing 'price' field for " + ticker);
                    continue;
                }

                double price = latest.get("price").asDouble();
                int pence = (int) Math.round(price * 100);

                String row = ticker + "," + pence + "\n";
                Files.writeString(pricesCsv, row, StandardOpenOption.APPEND);

                System.out.println("Imported " + ticker + " at " + pence + "p");

            } catch (Exception e) {
                System.err.println("Failed to import price for " + ticker + ": " + e.getMessage());
            }
        }

        System.out.println("Price import complete.");
    }
}