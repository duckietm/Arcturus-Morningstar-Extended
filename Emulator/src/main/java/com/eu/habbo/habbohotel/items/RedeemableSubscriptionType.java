package com.eu.habbo.habbohotel.items;

public enum RedeemableSubscriptionType {
    HABBO_CLUB("hc"),
    BUILDERS_CLUB("bc");

    public final String subscriptionType;

    RedeemableSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public static RedeemableSubscriptionType fromString(String subscriptionType) {
        if (subscriptionType == null) return null;

        switch (subscriptionType) {
            case "hc":
                return HABBO_CLUB;
            case "bc":
                return BUILDERS_CLUB;
        }

        return null;
    }
}
