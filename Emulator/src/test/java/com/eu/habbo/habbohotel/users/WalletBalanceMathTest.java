package com.eu.habbo.habbohotel.users;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletBalanceMathTest {
    @Test
    void creditsCanExceedTheSignedIntLimit() {
        assertEquals(5_000_000_000L, WalletBalanceMath.checkedBalance(4_999_999_999L, 1L));
    }

    @Test
    void acceptsRepresentableDepositsAndDebits() {
        assertEquals(150, WalletBalanceMath.checkedBalance(100, 50));
        assertEquals(25, WalletBalanceMath.checkedBalance(100, -75));
    }

    @Test
    void rejectsIntegerOverflowInsteadOfWrappingNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> WalletBalanceMath.checkedBalance(Integer.MAX_VALUE, 1));
    }

    @Test
    void rejectsOverdraftInsteadOfPersistingNegativeBalance() {
        assertThrows(IllegalArgumentException.class,
                () -> WalletBalanceMath.checkedBalance(100, -101));
    }

    @Test
    void rejectsAlreadyCorruptNegativeBalances() {
        assertThrows(IllegalArgumentException.class,
                () -> WalletBalanceMath.checkedBalance(-1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> WalletBalanceMath.requireValidBalance(-1));
    }

    @Test
    void clampedBalanceKeepsResultsInRangeWithoutThrowing() {
        assertEquals(150, WalletBalanceMath.clampedBalance(100, 50));
        assertEquals(0, WalletBalanceMath.clampedBalance(100, -101));
        assertEquals(Integer.MAX_VALUE, WalletBalanceMath.clampedBalance(Integer.MAX_VALUE, 1));
        assertEquals(0, WalletBalanceMath.clampedBalance(-5, 0));
    }
}
