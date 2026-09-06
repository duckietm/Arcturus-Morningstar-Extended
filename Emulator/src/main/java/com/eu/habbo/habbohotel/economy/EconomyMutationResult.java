package com.eu.habbo.habbohotel.economy;

/** A durable wallet mutation result with ABI-compatible legacy components. */
public record EconomyMutationResult(
        int balanceBefore,
        int balanceAfter,
        boolean applied,
        long balanceBeforeLong,
        long balanceAfterLong) {

    public EconomyMutationResult(int balanceBefore, int balanceAfter, boolean applied) {
        this(balanceBefore, balanceAfter, applied, balanceBefore, balanceAfter);
    }

    public EconomyMutationResult(long balanceBefore, long balanceAfter, boolean applied) {
        this(saturated(balanceBefore), saturated(balanceAfter), applied, balanceBefore, balanceAfter);
    }

    private static int saturated(long value) {
        return (int) Math.min(Integer.MAX_VALUE, value);
    }
}
