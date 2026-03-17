package uk.co.asepstrath.bank.capitalgains;

import java.time.LocalDate;

public class TaxYearInfo {
    public int startYear;        // e.g. 2024 for 2024/25
    public LocalDate startDate;  // 6 April startYear
    public LocalDate endDate;    // 5 April startYear+1
    public String label;         // "YYYY/YY"
    public boolean selected;
}

