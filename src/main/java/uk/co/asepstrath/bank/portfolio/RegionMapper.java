package uk.co.asepstrath.bank.portfolio;

public class RegionMapper {
    public static String Regionmap(String country) {
        if (country == null || country.isEmpty()) return "Unknown";

        switch (country.toLowerCase()) {
            case "uk":
            case "united kingdom":
            case "france":
            case "germany":
            case "switzerland":
            case "norway":
                return "Europe";

            case "united states":
            case "usa":
            case "canada":
            case "mexico":
                return "North America";

            case "japan":
            case "china":
            case "india":
            case "indonesia":
            case "australia":
            case "south korea":
                return "Asia Pacific";

            case "brazil":
            case "argentina":
                return "Latin America";

            case "south africa":
            case "kenya":
            case "uganda":
            case "egypt":
                return "africa";

            case "uae":
            case "united arab emirates":
            case "iran":
                return "Midle East";

            default:
                return "Unknown";

        }
    }
}
