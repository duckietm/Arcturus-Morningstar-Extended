package com.eu.habbo.habbohotel.economy;

/** Economy audit payload with 64-bit balances and released record components retained. */
public record EconomyAuditEntry(
        String operationId,
        int userId,
        Integer actorId,
        String operation,
        String reason,
        int currencyType,
        int amount,
        int balanceBefore,
        int balanceAfter,
        Integer itemId,
        String context,
        long balanceBeforeLong,
        long balanceAfterLong) {

    public EconomyAuditEntry(String operationId, int userId, Integer actorId, String operation, String reason,
            int currencyType, int amount, int balanceBefore, int balanceAfter, Integer itemId, String context) {
        this(operationId, userId, actorId, operation, reason, currencyType, amount, balanceBefore, balanceAfter,
                itemId, context, balanceBefore, balanceAfter);
    }

    public EconomyAuditEntry(String operationId, int userId, Integer actorId, String operation, String reason,
            int currencyType, int amount, long balanceBefore, long balanceAfter, Integer itemId, String context) {
        this(operationId, userId, actorId, operation, reason, currencyType, amount,
                saturated(balanceBefore), saturated(balanceAfter), itemId, context, balanceBefore, balanceAfter);
    }

    private static int saturated(long value) { return (int) Math.min(Integer.MAX_VALUE, value); }

    public static EconomyAuditEntry redemption(int userId, int itemId, int currencyType, int amount,
            int balanceBefore, int balanceAfter, String itemName) {
        return redemptionLong(userId, itemId, currencyType, amount, balanceBefore, balanceAfter, itemName);
    }

    public static EconomyAuditEntry redemptionLong(int userId, int itemId, int currencyType, int amount,
            long balanceBefore, long balanceAfter, String itemName) {
        if (userId <= 0 || itemId <= 0 || amount <= 0) throw new IllegalArgumentException("redemption audit values must be positive");
        return new EconomyAuditEntry("furniture-redeem:" + itemId, userId, userId, "furniture_redeem",
                "furniture.redeem", currencyType, amount, balanceBefore, balanceAfter, itemId, itemName == null ? "" : itemName);
    }

    public static EconomyAuditEntry from(EconomyOperation operation, int balanceBefore, int balanceAfter) {
        return fromLong(operation, balanceBefore, balanceAfter);
    }

    public static EconomyAuditEntry fromLong(EconomyOperation operation, long balanceBefore, long balanceAfter) {
        return new EconomyAuditEntry(operation.operationId(), operation.userId(), operation.actorId(), operation.operation(),
                operation.reason(), operation.currencyType(), operation.delta(), balanceBefore, balanceAfter,
                operation.itemId(), operation.context());
    }
}
