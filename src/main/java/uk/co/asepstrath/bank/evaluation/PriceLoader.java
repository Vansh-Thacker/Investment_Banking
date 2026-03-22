package uk.co.asepstrath.bank.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

import ru.justagod.scapi.models.PriceEntry;

public class PriceLoader {

    private final Map<String, TreeMap<LocalDateTime, Double>> priceMap = new HashMap<>();

    public PriceLoader(String csvPath) {
        load(csvPath);
    }

    private void load(String csvPath) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 3) continue;

                String ticker = p[0].trim().toUpperCase();
                LocalDateTime ts = LocalDateTime.parse(p[1].trim());
                double price = Double.parseDouble(p[2].trim());

                priceMap
                    .computeIfAbsent(ticker, k -> new TreeMap<>())
                    .put(ts, price);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load prices.csv", e);
        }
    }

    /** Returns the closest price at or before the timestamp */
    public Double getPrice(String ticker, LocalDateTime ts) {
        TreeMap<LocalDateTime, Double> series = priceMap.get(ticker);
        if (series == null) return null;

        Map.Entry<LocalDateTime, Double> entry = series.floorEntry(ts);
        return entry != null ? entry.getValue() : null;
    }

    public List<Double> getAllPricesForTicker(String ticker) {
        TreeMap<LocalDateTime, Double> series = priceMap.get(ticker.toUpperCase());
        if (series == null) return Collections.emptyList();
        return new ArrayList<>(series.values());
    }
}