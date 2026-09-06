package com.eu.habbo.habbohotel.rooms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Keeps emulator hand-item IDs inside Nitro's valid hand-item parameter range. */
public final class AvatarHandItemSupport {
    private static final String RESOURCE = "/avatar-supported-handitems.txt";
    private static final int MAX_HAND_ITEM_ID = 5000;
    private static final Set<Integer> SUPPORTED = loadSupportedIds();

    private AvatarHandItemSupport() {
    }

    public static boolean isSupported(int handItemId) {
        // CarryItem.params is an override table, not a whitelist. Nitro uses
        // its "default" parameter for ordinary legacy IDs omitted from that
        // table (including the canonical drinks 2, 5, 6 and 7). Reject only
        // values outside the renderer's bounded hand-item namespace.
        return handItemId >= 0 && handItemId <= MAX_HAND_ITEM_ID;
    }

    public static int normalize(int handItemId) {
        return isSupported(handItemId) ? handItemId : 0;
    }

    public static int supportedCount() {
        return SUPPORTED.size();
    }

    private static Set<Integer> loadSupportedIds() {
        InputStream stream = AvatarHandItemSupport.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing required hand-item registry " + RESOURCE);
        }

        Set<Integer> ids = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;

                int id = Integer.parseInt(value);
                if (id <= 0 || !ids.add(id)) {
                    throw new IllegalStateException("Invalid or duplicate hand-item ID in " + RESOURCE + ": " + value);
                }
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Cannot load hand-item registry " + RESOURCE, exception);
        }

        if (ids.isEmpty()) {
            throw new IllegalStateException("Hand-item registry is empty: " + RESOURCE);
        }

        return Collections.unmodifiableSet(ids);
    }
}
