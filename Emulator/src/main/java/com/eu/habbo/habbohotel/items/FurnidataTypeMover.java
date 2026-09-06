package com.eu.habbo.habbohotel.items;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Staff-only Furni Editor helper: move one existing furnidata object between
 * roomitemtypes and wallitemtypes while preserving the object itself.
 */
public final class FurnidataTypeMover {
    private static final Pattern CLASSNAME = Pattern.compile(
            "\\\"classname\\\"\\s*:\\s*\\\"((?:[^\\\"\\\\]|\\\\.)*+)\\\"",
            Pattern.CASE_INSENSITIVE);

    private FurnidataTypeMover() {}

    public static boolean move(
            Path source,
            long maxBytes,
            String classname,
            FurnitureType targetType) throws IOException {

        if (source == null || classname == null || targetType == null || !Files.isRegularFile(source)) {
            return false;
        }
        if (Files.size(source) > maxBytes) {
            throw new IOException("Furnidata source exceeds configured size limit");
        }

        String cn = classname.trim().toLowerCase(Locale.ROOT);
        if (cn.isEmpty()) return false;

        String raw = Files.readString(source, StandardCharsets.UTF_8);
        String edited = moveInDocument(raw, cn, targetType);
        if (edited == null || edited.equals(raw)) return false;

        Path backup = source.resolveSibling(source.getFileName() + ".bak.type." + System.nanoTime());
        Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);

        Path temp = source.resolveSibling(source.getFileName() + ".tmp.type." + System.nanoTime());
        Files.writeString(temp, edited, StandardCharsets.UTF_8);

        try {
            Files.move(temp, source, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, source, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }

        return true;
    }

    static String moveInDocument(String raw, String cn, FurnitureType targetType) {
        Matcher matcher = CLASSNAME.matcher(raw);
        int objectStart = -1;
        int objectEnd = -1;

        while (matcher.find()) {
            if (!matcher.group(1).trim().toLowerCase(Locale.ROOT).equals(cn)) continue;
            objectStart = objectStart(raw, matcher.start());
            objectEnd = matchingObjectClose(raw, objectStart);
            break;
        }

        if (objectStart < 0 || objectEnd < 0) return null;

        String targetSection =
                targetType == FurnitureType.WALL ? "wallitemtypes" : "roomitemtypes";

        int targetOpen = arrayOpen(raw, targetSection);
        int targetClose = matchingArrayClose(raw, targetOpen - 1);
        if (targetOpen < 0 || targetClose < 0) return null;

        if (objectStart > targetOpen && objectEnd < targetClose) {
            return raw; // already classified correctly
        }

        String object = raw.substring(objectStart, objectEnd + 1);

        int removeStart = objectStart;
        int removeEnd = objectEnd + 1;

        int right = removeEnd;
        while (right < raw.length() && Character.isWhitespace(raw.charAt(right))) right++;

        if (right < raw.length() && raw.charAt(right) == ',') {
            removeEnd = right + 1;
        } else {
            int left = removeStart - 1;
            while (left >= 0 && Character.isWhitespace(raw.charAt(left))) left--;
            if (left >= 0 && raw.charAt(left) == ',') removeStart = left;
        }

        String without = raw.substring(0, removeStart) + raw.substring(removeEnd);
        int insertAt = arrayOpen(without, targetSection);
        if (insertAt < 0) return null;

        return without.substring(0, insertAt)
                + "\n"
                + object
                + ","
                + without.substring(insertAt);
    }

    private static int arrayOpen(String raw, String section) {
        int sectionIndex = raw.indexOf("\"" + section + "\"");
        if (sectionIndex < 0) return -1;
        int furnitype = raw.indexOf("\"furnitype\"", sectionIndex);
        if (furnitype < 0) return -1;

        boolean inString = false;
        for (int i = furnitype; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
            } else if (c == '"') {
                inString = true;
            } else if (c == '[') {
                return i + 1;
            }
        }
        return -1;
    }

    private static int objectStart(String raw, int from) {
        int depth = 0;
        for (int i = from; i >= 0; i--) {
            char c = raw.charAt(i);
            if (c == '}') depth++;
            else if (c == '{') {
                if (depth == 0) return i;
                depth--;
            }
        }
        return -1;
    }

    private static int matchingObjectClose(String raw, int open) {
        if (open < 0) return -1;
        int depth = 0;
        boolean inString = false;

        for (int i = open; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return i;
        }
        return -1;
    }

    private static int matchingArrayClose(String raw, int open) {
        if (open < 0 || open >= raw.length() || raw.charAt(open) != '[') return -1;
        int depth = 0;
        boolean inString = false;

        for (int i = open; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '[') depth++;
            else if (c == ']' && --depth == 0) return i;
        }
        return -1;
    }
}
