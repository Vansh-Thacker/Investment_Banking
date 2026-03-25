package uk.co.asepstrath.bank.cash;

import  org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CashTransactionTest {
    @Test // does the constructor work for deposits
    void testDepositConstructor() {
        LocalDateTime before = LocalDateTime.now();
        CashTransaction tx = new CashTransaction(500, "DEPOSIT", 1000);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(500, tx.getAmount());
        assertEquals("DEPOSIT", tx.getType());
        assertEquals(1500, tx.getBalanceAfter()); // 1000 + 500

        assertTrue(!tx.getDateTime().isBefore(before));
        assertTrue(!tx.getDateTime().isAfter(after));
    }

    @Test //does the constructor work for withdrawal
    void testWithdrawalConstructor() {
        CashTransaction tx = new CashTransaction(300, "WITHDRAWAL", 1000);

        assertEquals(300, tx.getAmount());
        assertEquals("WITHDRAWAL", tx.getType());
        assertEquals(700, tx.getBalanceAfter()); // 1000 - 300
    }
    @Test //does csv loading work correctly
    void testCsvConstructor() {
        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 10, 0);

        CashTransaction tx = new CashTransaction(time, "DEPOSIT", 200, 1200);

        assertEquals(time, tx.getDateTime());
        assertEquals("DEPOSIT", tx.getType());
        assertEquals(200, tx.getAmount());
        assertEquals(1200, tx.getBalanceAfter());
    }

    @Test // does the getter return the correct amount
    void testGetAmount() {
        CashTransaction tx = new CashTransaction(250, "DEPOSIT", 1000);
        assertEquals(250, tx.getAmount());
    }

    @Test // does the getter return the correct type
    void testGetType() {
        CashTransaction tx = new CashTransaction(250, "WITHDRAWAL", 1000);
        assertEquals("WITHDRAWAL", tx.getType());
    }

    @Test // does the getter return the correct bal after the transaction
    void testGetBalanceAfter() {
        CashTransaction tx = new CashTransaction(400, "DEPOSIT", 600);
        assertEquals(1000, tx.getBalanceAfter());
    }

    @Test // does the date and time getter return the correct stuff
    void testGetDateTime() {
        LocalDateTime before = LocalDateTime.now();
        CashTransaction tx = new CashTransaction(100, "DEPOSIT", 0);
        LocalDateTime after = LocalDateTime.now();

        assertTrue(!tx.getDateTime().isBefore(before));
        assertTrue(!tx.getDateTime().isAfter(after));
    }

}
