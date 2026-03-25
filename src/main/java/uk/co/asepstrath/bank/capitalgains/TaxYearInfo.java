package uk.co.asepstrath.bank.capitalgains;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaxYearInfo {

    public int startYear;     // e.g. 2024
    public int endYear;       // e.g. 2025
    public String label;      // "2024/25"
    public boolean selected;  // used by your dropdown

    public TaxYearInfo(int startYear) {
        this.startYear = startYear;
        this.endYear = startYear + 1;
        this.label = startYear + "/" + String.valueOf(endYear).substring(2);
    }

    /** Create a TaxYearInfo for a given start year */
    public static TaxYearInfo forYear(int startYear) {
        return new TaxYearInfo(startYear);
    }

    /** UK tax year starts on 6 April */
    public static int getTaxYearStart(LocalDate date) {
        int year = date.getYear();
        LocalDate taxStart = LocalDate.of(year, 4, 6);

        // If before 6 April, the tax year started last year
        if (date.isBefore(taxStart)) {
            return year - 1;
        }
        return year;
    }

    /** Overload for LocalDateTime */
    public static int getTaxYearStart(LocalDateTime dt) {
        return getTaxYearStart(dt.toLocalDate());
    }

    /** Current tax year start */
    public static int getCurrentTaxYearStart() {
        return getTaxYearStart(LocalDate.now());
    }

    /** Check if a timestamp is inside this tax year */
    public boolean contains(LocalDateTime dt) {
        return getTaxYearStart(dt) == this.startYear;
    }
}



