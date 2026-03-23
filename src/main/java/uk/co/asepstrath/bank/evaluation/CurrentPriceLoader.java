package uk.co.asepstrath.bank.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;


public class CurrentPriceLoader {

    private final Map<String, Integer> currentPrices = new HashMap<>();

    public CurrentPriceLoader(String csvPath) {
        load(csvPath);
    }

    private void load(String csvPath) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 2) continue;

                String ticker = p[0].trim().toUpperCase();
                int pricePence = Integer.parseInt(p[1].trim());

                currentPrices.put(ticker, pricePence);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load currentprices.csv", e);
        }
    }

    public Integer getCurrentPrice(String ticker) {
        return currentPrices.get(ticker.toUpperCase());
    }
}