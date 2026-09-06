package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test-mode clothing validation - the looks counterpart of {@link FurniValidateCommand}. Cross-checks
 * the three files that have to agree before a clothing item can draw: FigureData (the sets the
 * wardrobe offers), FigureMap (which library each part lives in) and the figure/*.nitro bundles.
 *
 * <p>{@code :validatelooks} checks the look you are wearing, which is what you are looking at when a
 * part of an avatar comes up missing; {@code :validatelooks all} sweeps every set in FigureData. The
 * full report is written next to the emulator under {@code logging/}.
 */
public class LooksValidateCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(LooksValidateCommand.class);

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final int WHISPERED_DETAILS = 12;

    /** One problem with one clothing set. */
    private record Finding(String code, String setType, String setId, String detail) {}

    private static final List<String> CODES = List.of(
            "DEAD_SET",
            "NESTED_LIBRARY",
            "LIBRARY_WITHOUT_ART",
            "GHOST_LAYER",
            "SET_WITHOUT_PARTS");

    public LooksValidateCommand() {
        super(
                "cmd_furnidata",
                Emulator.getTexts()
                        .getValue("commands.keys.cmd_validatelooks", "validatelooks;lookcheck;testlook")
                        .split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Habbo habbo = gameClient.getHabbo();
        boolean everything = params.length > 1 && "all".equalsIgnoreCase(params[1]);

        Path assets = assetBase();

        if (assets == null) {
            habbo.whisper("furni.editor.asset.base.path non punta ai file del client: impossibile controllare i look.");

            return true;
        }

        JsonObject figureData = readJson(assets.resolve("FigureData.json"));
        JsonObject figureMap = readJson(assets.resolve("FigureMap.json"));

        if (figureData == null || figureMap == null) {
            habbo.whisper("FigureData.json o FigureMap.json non leggibili in " + assets + ".");

            return true;
        }

        Index index = new Index(figureMap, assets.resolve("figure"));
        Set<String> wanted = everything ? null : setsInLook(habbo.getHabboInfo().getLook());
        List<Finding> findings = validate(figureData, index, wanted);

        report(habbo, findings, index, everything, wanted);

        return true;
    }

    // ------------------------------------------------------------------ the index

    /** Which library each (part id, part type) lives in, and which libraries actually have art. */
    private static final class Index {

        private final Map<String, String> libraryForPart = new HashMap<>();
        private final Set<String> withoutArt = new HashSet<>();
        private final List<String> nested = new ArrayList<>();
        private int libraries;

        Index(JsonObject figureMap, Path figureDirectory) {
            JsonArray libraries = figureMap.getAsJsonArray("libraries");

            if (libraries == null) {
                return;
            }

            for (JsonElement element : libraries) {
                if (element.isJsonObject()) {
                    this.read(element.getAsJsonObject(), figureDirectory, null);
                }
            }
        }

        private void read(JsonObject library, Path figureDirectory, String parent) {
            String id = library.has("id") ? library.get("id").getAsString() : null;

            if (id == null) {
                return;
            }

            this.libraries++;

            if (parent != null) {
                this.nested.add(id + " dentro " + parent);
            }

            if (!Files.isRegularFile(figureDirectory.resolve(id + ".nitro"))) {
                this.withoutArt.add(id);
            }

            JsonArray parts = library.getAsJsonArray("parts");

            if (parts == null) {
                return;
            }

            for (JsonElement element : parts) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject part = element.getAsJsonObject();

                // A library filed inside another library's parts: the renderer reads part.type on
                // every entry here, so everything such an entry carries is unreachable.
                if (!part.has("type") && part.has("parts")) {
                    this.read(part, figureDirectory, id);

                    continue;
                }

                if (!part.has("type") || !part.has("id")) {
                    continue;
                }

                String key = part.get("id").getAsString() + ":" + part.get("type").getAsString();
                String current = this.libraryForPart.get(key);

                // Art beats no art, so a duplicate mapping cannot hide a working library.
                if (current == null || (this.withoutArt.contains(current) && !this.withoutArt.contains(id))) {
                    this.libraryForPart.put(key, id);
                }
            }
        }

        boolean resolves(String partId, String partType) {
            String library = this.libraryForPart.get(partId + ":" + partType);

            return library != null && !this.withoutArt.contains(library);
        }
    }

    // ------------------------------------------------------------------ the checks

    private List<Finding> validate(JsonObject figureData, Index index, Set<String> wanted) {
        List<Finding> findings = new ArrayList<>();
        JsonArray setTypes = figureData.getAsJsonArray("setTypes");

        if (setTypes == null) {
            return findings;
        }

        for (String nested : index.nested) {
            findings.add(new Finding("NESTED_LIBRARY", "-", "-",
                    nested + ": libreria annidata nei parts di un'altra, non viene mai registrata"));
        }

        for (String library : index.withoutArt) {
            findings.add(new Finding("LIBRARY_WITHOUT_ART", "-", "-", library + ".nitro non esiste"));
        }

        for (JsonElement element : setTypes) {
            JsonObject setType = element.getAsJsonObject();
            String type = setType.has("type") ? setType.get("type").getAsString() : "?";
            JsonArray sets = setType.getAsJsonArray("sets");

            if (sets == null) {
                continue;
            }

            for (JsonElement setElement : sets) {
                JsonObject set = setElement.getAsJsonObject();
                String id = set.has("id") ? set.get("id").getAsString() : "?";

                if (wanted != null && !wanted.contains(type + "-" + id)) {
                    continue;
                }

                JsonArray parts = set.getAsJsonArray("parts");

                if (parts == null || parts.isEmpty()) {
                    findings.add(new Finding("SET_WITHOUT_PARTS", type, id, "il set non ha parti"));

                    continue;
                }

                List<String> dead = new ArrayList<>();
                boolean primaryAlive = false;

                for (JsonElement partElement : parts) {
                    JsonObject part = partElement.getAsJsonObject();
                    String partType = part.get("type").getAsString();
                    String partId = part.get("id").getAsString();

                    if (index.resolves(partId, partType)) {
                        primaryAlive |= partType.equals(type);
                    } else {
                        dead.add(partType + ":" + partId);
                    }
                }

                if (!primaryAlive) {
                    boolean selectable = set.has("selectable") && set.get("selectable").getAsBoolean();
                    findings.add(new Finding("DEAD_SET", type, id,
                            "nessuna arte per la parte " + type + (selectable ? " ed e' ancora selezionabile" : "")));
                } else if (!dead.isEmpty()) {
                    findings.add(new Finding("GHOST_LAYER", type, id, "livelli senza arte: " + String.join(", ", dead)));
                }
            }
        }

        findings.sort((a, b) -> {
            int order = Integer.compare(CODES.indexOf(a.code()), CODES.indexOf(b.code()));

            return order != 0 ? order : a.setId().compareTo(b.setId());
        });

        return findings;
    }

    /** The "<type>-<set id>" pairs a figure string wears, e.g. hr-828-45.hd-180-2 */
    private static Set<String> setsInLook(String look) {
        Set<String> sets = new HashSet<>();

        if (look == null || look.isBlank()) {
            return sets;
        }

        for (String piece : look.split("\\.")) {
            String[] fields = piece.split("-");

            if (fields.length >= 2) {
                sets.add(fields[0] + "-" + fields[1]);
            }
        }

        return sets;
    }

    // ------------------------------------------------------------------ the sources

    private static Path assetBase() {
        String base = Emulator.getConfig().getValue("furni.editor.asset.base.path", "");

        if (base.isEmpty()) {
            return null;
        }

        Path path = Paths.get(base);

        return Files.isDirectory(path) ? path : null;
    }

    private static JsonObject readJson(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return null;
            }

            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            LOGGER.error("Looks validation could not read {}", path, exception);

            return null;
        }
    }

    // ------------------------------------------------------------------ the report

    private void report(Habbo habbo, List<Finding> findings, Index index, boolean everything, Set<String> wanted) {
        Map<String, Integer> counts = new TreeMap<>();

        for (Finding finding : findings) {
            counts.merge(finding.code(), 1, Integer::sum);
        }

        StringBuilder chat = new StringBuilder("<b>Validazione look</b> (")
                .append(everything ? "tutti i set" : "il tuo look")
                .append(")\r")
                .append("- librerie FigureMap: ").append(index.libraries).append("\r")
                .append("- parti mappate: ").append(index.libraryForPart.size()).append("\r");

        if (!everything && wanted != null) {
            chat.append("- set indossati: ").append(wanted.size()).append("\r");
        }

        if (findings.isEmpty()) {
            chat.append("\rNessun problema: ogni parte ha la sua arte.");
            habbo.whisper(chat.toString());

            return;
        }

        chat.append("- problemi: ").append(findings.size()).append("\r\r");

        for (String code : CODES) {
            Integer count = counts.get(code);

            if (count != null) {
                chat.append(code).append(": ").append(count).append("\r");
            }
        }

        chat.append("\r");

        for (Finding finding : findings.subList(0, Math.min(WHISPERED_DETAILS, findings.size()))) {
            chat.append("[").append(finding.code()).append("] ")
                    .append(finding.setType()).append(" set ").append(finding.setId())
                    .append(": ").append(finding.detail()).append("\r");
        }

        if (findings.size() > WHISPERED_DETAILS) {
            chat.append("... e altri ").append(findings.size() - WHISPERED_DETAILS).append("\r");
        }

        Path written = writeReport(findings, index, everything);

        if (written != null) {
            chat.append("\rReport completo: ").append(written.toAbsolutePath());
        }

        habbo.whisper(chat.toString());
    }

    private Path writeReport(List<Finding> findings, Index index, boolean everything) {
        try {
            Path directory = Paths.get("logging");
            Files.createDirectories(directory);

            Path file = directory.resolve("looks-validation-" + LocalDateTime.now().format(STAMP) + ".txt");
            StringBuilder out = new StringBuilder();
            out.append("Looks validation - ").append(everything ? "all sets" : "current look").append('\n')
                    .append("generated: ").append(LocalDateTime.now()).append('\n')
                    .append("figuremap libraries: ").append(index.libraries).append('\n')
                    .append("mapped parts: ").append(index.libraryForPart.size()).append('\n')
                    .append("problems: ").append(findings.size()).append("\n\n");

            for (Finding finding : findings) {
                out.append(finding.code()).append('\t')
                        .append(finding.setType()).append('\t')
                        .append(finding.setId()).append('\t')
                        .append(finding.detail()).append('\n');
            }

            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);

            return file;
        } catch (Exception exception) {
            LOGGER.error("Looks validation could not write its report", exception);

            return null;
        }
    }
}
