package uk.co.asepstrath.bank.apiimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.*;

public class TransactionImporter {

    private static final String ROOT = "https://api.asep-strath.co.uk";

    public static void importTransactions() throws Exception {
        String nextToken = null;

        do {
            // Build URL with pagination
            String url = ROOT + "/api/transactions";
            if (nextToken != null) {
                url += "?nextToken=" + nextToken;
            }

            String json = ApiReader.get(url);
            System.out.println("RAW TX JSON:\n" + json);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode transactions = root.get("transactions");
            if (transactions == null || !transactions.isArray()) {
                System.out.println("No transactions found in this page.");
                return;
            }

            for (JsonNode tx : transactions) {

                String id = tx.get("id").asText();
                String timestamp = tx.get("timestamp").asText();
                String type = tx.get("type").asText();
                String investorId = tx.get("investorId").asText();

                // ticker can be null → must handle safely
                JsonNode tickerNode = tx.get("ticker");
                String ticker = (tickerNode == null || tickerNode.isNull())
                        ? ""
                        : tickerNode.asText();

                double amount = tx.get("totalCashAmount").asDouble();
                int pence = (int) Math.round(amount * 100);

                // Write to UserStats/<id>/history.csv
                Path history = Path.of("UserStats", investorId, "history.csv");
                Files.createDirectories(history.getParent());

                if (!Files.exists(history)) {
                    Files.writeString(history, "date,type,ticker,amount,balanceAfter\n");
                }

                // balanceAfter is unknown from API → set to 0 for now
                String row = String.format(
                        "%s,%s,%s,%d,%d%n",
                        timestamp, type, ticker, pence, 0
                );

                Files.writeString(history, row, StandardOpenOption.APPEND);
            }

            // Handle pagination
            JsonNode pagination = root.get("pagination");
            if (pagination != null && pagination.has("nextToken")) {
                nextToken = pagination.get("nextToken").asText();
            } else {
                nextToken = null;
            }

        } while (nextToken != null);
    }
}