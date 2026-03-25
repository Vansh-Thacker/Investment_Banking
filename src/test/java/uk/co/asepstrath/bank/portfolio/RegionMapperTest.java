package uk.co.asepstrath.bank.portfolio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RegionMapperTest {

    // ─── Europe ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "uk,            Europe",
            "United Kingdom,Europe",
            "france,        Europe",
            "Germany,       Europe",
            "Switzerland,   Europe",
            "Norway,        Europe"
    })
    @DisplayName("European countries map to Europe")
    void testEuropeanCountries(String country, String expected) {
        assertEquals(expected, RegionMapper.Regionmap(country));
    }

    // ─── North America ───────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "United States, North America",
            "usa,           North America",
            "Canada,        North America",
            "Mexico,        North America"
    })
    @DisplayName("North American countries map to North America")
    void testNorthAmericanCountries(String country, String expected) {
        assertEquals(expected, RegionMapper.Regionmap(country));
    }

    // ─── Asia Pacific ────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "Japan,       Asia Pacific",
            "China,       Asia Pacific",
            "India,       Asia Pacific",
            "Indonesia,   Asia Pacific",
            "Australia,   Asia Pacific",
            "South Korea, Asia Pacific"
    })
    @DisplayName("Asia Pacific countries map correctly")
    void testAsiaPacificCountries(String country, String expected) {
        assertEquals(expected, RegionMapper.Regionmap(country));
    }

    // ─── Latin America ───────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "Brazil,    Latin America",
            "Argentina, Latin America"
    })
    @DisplayName("Latin American countries map correctly")
    void testLatinAmericanCountries(String country, String expected) {
        assertEquals(expected, RegionMapper.Regionmap(country));
    }

    // ─── Africa ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "South Africa, Africa",
            "Kenya,        Africa",
            "Uganda,       Africa",
            "Egypt,        Africa"
    })
    @DisplayName("African countries map to africa")
    void testAfricanCountries(String country, String expected) {
        assertEquals(expected, RegionMapper.Regionmap(country));
    }

    // ─── Middle East ─────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "UAE,                  Middle East",
            "United Arab Emirates, Middle East",
            "Iran,                 Middle East"
    })
    @DisplayName("Middle East countries map to Midle East")
    void testMiddleEastCountries(String country, String expected) {
        assertEquals(expected, RegionMapper.Regionmap(country));
    }

    // ─── Unknown / Edge Cases ────────────────────────────────────────────────

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Null and empty country returns Unknown")
    void testNullAndEmptyReturnsUnknown(String country) {
        assertEquals("Unknown", RegionMapper.Regionmap(country));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mars", "Atlantis", "Unknown", "XYZ", "12345", "!@#$%"})
    @DisplayName("Unrecognised countries return Unknown")
    void testUnknownCountriesReturnUnknown(String country) {
        assertEquals("Unknown", RegionMapper.Regionmap(country));
    }

    @Test
    @DisplayName("Case insensitive matching works for United Kingdom")
    void testCaseInsensitiveUnitedKingdom() {
        assertEquals("Europe", RegionMapper.Regionmap("UNITED KINGDOM"));
        assertEquals("Europe", RegionMapper.Regionmap("united kingdom"));
        assertEquals("Europe", RegionMapper.Regionmap("United Kingdom"));
    }

    @Test
    @DisplayName("Case insensitive matching works for United States")
    void testCaseInsensitiveUnitedStates() {
        assertEquals("North America", RegionMapper.Regionmap("UNITED STATES"));
        assertEquals("North America", RegionMapper.Regionmap("united states"));
    }

    @Test
    @DisplayName("Case insensitive matching works for Japan")
    void testCaseInsensitiveJapan() {
        assertEquals("Asia Pacific", RegionMapper.Regionmap("JAPAN"));
        assertEquals("Asia Pacific", RegionMapper.Regionmap("japan"));
    }

    @Test
    @DisplayName("Whitespace-only string returns Unknown")
    void testWhitespaceReturnsUnknown() {
        assertEquals("Unknown", RegionMapper.Regionmap("   "));
    }

    @Test
    @DisplayName("UK alias maps same as United Kingdom")
    void testUKAliasMatchesUnitedKingdom() {
        assertEquals(RegionMapper.Regionmap("uk"),
                RegionMapper.Regionmap("United Kingdom"));
    }

    @Test
    @DisplayName("USA alias maps same as United States")
    void testUSAAliasMatchesUnitedStates() {
        assertEquals(RegionMapper.Regionmap("usa"),
                RegionMapper.Regionmap("United States"));
    }

    @Test
    @DisplayName("UAE alias maps same as United Arab Emirates")
    void testUAEAliasMatchesUnitedArabEmirates() {
        assertEquals(RegionMapper.Regionmap("uae"),
                RegionMapper.Regionmap("United Arab Emirates"));
    }

    @Test
    @DisplayName("Return value is never null")
    void testReturnValueIsNeverNull() {
        assertNotNull(RegionMapper.Regionmap(null));
        assertNotNull(RegionMapper.Regionmap(""));
        assertNotNull(RegionMapper.Regionmap("Unknown Country"));
        assertNotNull(RegionMapper.Regionmap("United Kingdom"));
    }
}