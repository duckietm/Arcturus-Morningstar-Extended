package com.eu.habbo.habbohotel.gamedata;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.FurnitureTextProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Edits single keys of the client's ExternalTexts.json (the Nitro localisation file served next to the
 * furnidata) without re-serialising the whole document, so formatting and key order stay untouched.
 */
public final class ExternalTextsWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalTextsWriter.class);
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final int BACKUP_KEEP = 5;
    private static final String FILE_NAME = "ExternalTexts.json";

    private ExternalTextsWriter() {}

    /** The texts file: {@code external.texts.path} when configured, else the one beside the furnidata source. */
    public static Path resolvePath() {
        String configured = Emulator.getConfig().getValue("external.texts.path", "");
        if (configured != null && !configured.isBlank()) return Path.of(configured.trim());

        FurnitureTextProvider provider = Emulator.getGameEnvironment().getFurnitureTextProvider();
        if (provider == null || provider.getSource() == null) return null;

        Path source = provider.getSource().toAbsolutePath();
        Path directory = provider.isSourceDirectory() ? source : source.getParent();

        for (Path current = directory; current != null; current = current.getParent()) {
            Path candidate = current.resolve(FILE_NAME);
            if (Files.exists(candidate)) return candidate;
        }

        return null;
    }

    /** Sets {@code key} to {@code value} (replacing the existing line or inserting a new one at the top). */
    public static void write(String key, String value) throws IOException {
        Path target = resolvePath();
        if (target == null) throw new IOException(FILE_NAME + " not found (set external.texts.path)");

        LOCK.lock();
        try {
            String content = Files.readString(target, StandardCharsets.UTF_8);
            String json = jsonString(value);
            Pattern pattern = Pattern.compile("(\"" + Pattern.quote(key) + "\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"");
            Matcher matcher = pattern.matcher(content);
            String edited;

            if (matcher.find()) {
                edited = content.substring(0, matcher.start()) + matcher.group(1) + json + content.substring(matcher.end());
            } else {
                int brace = content.indexOf('{');
                if (brace < 0) throw new IOException(FILE_NAME + " is not a JSON object");

                String eol = content.contains("\r\n") ? "\r\n" : "\n";
                String rest = content.substring(brace + 1);
                boolean empty = rest.trim().startsWith("}");
                edited = content.substring(0, brace + 1) + eol + "    \"" + key + "\": " + json + (empty ? "" : ",") + rest;
            }

            backup(target);
            atomicWrite(target, edited);
        } finally {
            LOCK.unlock();
        }
    }

    private static String jsonString(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) builder.append(String.format("\\u%04x", (int) c));
                    else builder.append(c);
                }
            }
        }
        return builder.append('"').toString();
    }

    private static void backup(Path target) throws IOException {
        Path bak = target.resolveSibling(target.getFileName() + ".bak." + System.nanoTime());
        Files.copy(target, bak, StandardCopyOption.REPLACE_EXISTING);

        String prefix = target.getFileName() + ".bak.";
        List<Path> baks = new ArrayList<>();
        try (Stream<Path> stream = Files.list(target.getParent())) {
            stream.filter(p -> p.getFileName().toString().startsWith(prefix)).forEach(baks::add);
        }
        baks.sort((a, b) -> Long.compare(stamp(a), stamp(b)));
        for (int i = 0; i < baks.size() - BACKUP_KEEP; i++) Files.deleteIfExists(baks.get(i));
    }

    private static long stamp(Path path) {
        String name = path.getFileName().toString();
        try {
            return Long.parseLong(name.substring(name.lastIndexOf('.') + 1));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static void atomicWrite(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp." + System.nanoTime());
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.info("External texts updated: {}", target);
    }
}
