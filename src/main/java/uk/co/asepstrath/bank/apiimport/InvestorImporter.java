package uk.co.asepstrath.bank.apiimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.*;

public class InvestorImporter {

    private static final String ROOT = "https://api.asep-strath.co.uk";

    public static void importInvestors() throws Exception {
        String json = ApiReader.get(ROOT + "/api/investors");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode investors = root.get("investors");

        Path usersCsv = Path.of("users.csv");

        if (!Files.exists(usersCsv)) {
            Files.writeString(usersCsv, "id,name,passwordHash,balancePence\n");
        }

        for (JsonNode inv : investors) {
            String id = inv.get("id").asText();
            String name = inv.get("name").asText();
            double bal = inv.get("cashBalance").asDouble();
            int pence = (int) Math.round(bal * 100);

            // Add user to users.csv
            String row = String.format("%s,%s,,%d%n", id, name, pence);
            Files.writeString(usersCsv, row, StandardOpenOption.APPEND);

            // Create user folder + empty history.csv
            Path history = Path.of("UserStats", id, "history.csv");
            Files.createDirectories(history.getParent());
            if (!Files.exists(history)) {
                Files.writeString(history, "date,type,ticker,amount,balanceAfter\n");
            }
        }
    }
}