package com.eu.habbo.habbohotel.rooms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Keeps emulator effect grants aligned with the Nitro effect bundles we actually deploy. */
public final class AvatarEffectSupport {
    private static final String RESOURCE = "/avatar-supported-effects.txt";
    private static final Set<Integer> SUPPORTED = loadSupportedIds();

    private AvatarEffectSupport() {
    }

    public static boolean isSupported(int effectId) {
        return effectId == 0 || SUPPORTED.contains(effectId);
    }

    public static int normalize(int effectId) {
        return isSupported(effectId) ? effectId : 0;
    }

    public static int supportedCount() {
        return SUPPORTED.size();
    }

    public static Set<Integer> supportedIds() {
        return SUPPORTED;
    }

    private static Set<Integer> loadSupportedIds() {
        InputStream stream = AvatarEffectSupport.class.getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing required effect registry " + RESOURCE);

        Set<Integer> ids = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;

                int id = Integer.parseInt(value);
                if (id <= 0 || !ids.add(id)) {
                    throw new IllegalStateException("Invalid or duplicate effect ID in " + RESOURCE + ": " + value);
                }
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Cannot load effect registry " + RESOURCE, exception);
        }

        if (ids.isEmpty()) throw new IllegalStateException("Effect registry is empty: " + RESOURCE);
        return Collections.unmodifiableSet(ids);
    }
}
