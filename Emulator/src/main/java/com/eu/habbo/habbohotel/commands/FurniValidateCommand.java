package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.FurnidataEntry;
import com.eu.habbo.habbohotel.items.FurnidataReader;
import com.eu.habbo.habbohotel.items.FurnidataSourceResolver;
import com.eu.habbo.habbohotel.items.FurnitureTextProvider;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test-mode furni validation. Cross-checks the three files that have to agree before a furni can
 * draw - items_base, the furnidata the renderer reads, and the .nitro bundle on disk - and reports
 * every row that would render as a placeholder or as the wrong furni.
 *
 * <p>{@code :validatefurni} checks the furni placed in the current room, which is the thing you are
 * actually looking at when one comes up as a placeholder; {@code :validatefurni all} sweeps every
 * items_base row. The full report is written next to the emulator under {@code logging/}.
 */
public class FurniValidateCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(FurniValidateCommand.class);

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** Pets live in items_base as "a0 pet<n>" rows with no art and no furnidata, by design. */
    private static final Pattern PET_BASE = Pattern.compile("a0 pet\\d+", Pattern.CASE_INSENSITIVE);

    /** Every type code items_base is allowed to carry; anything else loads as a floor furni. */
    private static final Set<String> VALID_TYPES = Set.of("s", "i", "e", "b", "r", "h", "p");

    /** How many problems are read out in chat before the reader is pointed at the report. */
    private static final int WHISPERED_DETAILS = 12;

    /**
     * One problem found with one items_base row. {@code code} is the machine-readable reason, in
     * severity order: the first two are what make the client draw a placeholder.
     */
    private record Finding(String code, int baseId, String classname, int spriteId, String detail) {}

    private static final List<String> CODES = List.of(
            "NO_FURNIDATA",
            "MISSING_BUNDLE",
            "CLASSNAME_MISMATCH",
            "DUPLICATE_FURNIDATA_ID",
            "DUPLICATE_SPRITE_ID",
            "BAD_TYPE",
            "TYPE_MISMATCH",
            "GHOST_CLASSNAME",
            "PLACEHOLDER_NAME",
            "WIRED_BLOCKS_TILE");

    public FurniValidateCommand() {
        super(
                "cmd_furnidata",
                Emulator.getTexts()
                        .getValue("commands.keys.cmd_validatefurni", "validatefurni;furnicheck;testfurni")
                        .split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Habbo habbo = gameClient.getHabbo();
        boolean everything = params.length > 1 && "all".equalsIgnoreCase(params[1]);

        Map<Integer, Item> scope = everything ? allBaseItems() : baseItemsInRoom(habbo);

        if (scope.isEmpty()) {
            habbo.whisper(everything
                    ? "Nessun furni da controllare: items_base e' vuoto."
                    : "Nessun furni in questa stanza da controllare. Usa :validatefurni all per l'intero hotel.");

            return true;
        }

        List<FurnidataEntry> furnidata = readFurnidata();

        if (furnidata.isEmpty()) {
            habbo.whisper("Furnidata non leggibile: controlla items.furnidata.path / furni.editor.asset.base.path.");

            return true;
        }

        Path furnitureDirectory = furnitureDirectory();
        List<Finding> findings = validate(scope, furnidata, furnitureDirectory, everything);

        report(habbo, findings, scope.size(), furnidata.size(), furnitureDirectory, everything);

        return true;
    }

    // ------------------------------------------------------------------ the checks

    private List<Finding> validate(
            Map<Integer, Item> scope, List<FurnidataEntry> furnidata, Path furnitureDirectory, boolean everything) {
        Map<Integer, List<FurnidataEntry>> byId = new HashMap<>();

        for (FurnidataEntry entry : furnidata) {
            byId.computeIfAbsent(entry.id(), id -> new ArrayList<>()).add(entry);
        }

        Map<Integer, List<Item>> bySprite = new HashMap<>();

        for (Item item : allBaseItems().values()) {
            bySprite.computeIfAbsent(item.getSpriteId(), sprite -> new ArrayList<>()).add(item);
        }

        Map<Integer, String> rawTypes = everything ? rawTypes() : Map.of();
        Set<String> bundlesChecked = new HashSet<>();
        List<Finding> findings = new ArrayList<>();

        for (Item item : scope.values()) {
            if (!isRoomFurni(item)) {
                continue;
            }

            String classname = item.getName() == null ? "" : item.getName();
            List<FurnidataEntry> entries = byId.getOrDefault(item.getSpriteId(), List.of());

            if (entries.isEmpty()) {
                findings.add(new Finding("NO_FURNIDATA", item.getId(), classname, item.getSpriteId(),
                        "nessuna voce furnidata per questo sprite id: il client disegna un placeholder"));
            } else {
                if (entries.size() > 1) {
                    findings.add(new Finding("DUPLICATE_FURNIDATA_ID", item.getId(), classname, item.getSpriteId(),
                            entries.size() + " voci furnidata con lo stesso id ("
                                    + entries.stream().map(FurnidataEntry::classname).distinct().toList()
                                    + "): vince l'ultima letta"));
                }

                boolean matches = entries.stream().anyMatch(entry -> classname.equals(entry.classname()));

                if (!matches) {
                    findings.add(new Finding("CLASSNAME_MISMATCH", item.getId(), classname, item.getSpriteId(),
                            "furnidata dice " + entries.get(0).classname() + ": viene disegnato il furni sbagliato"));
                }

                FurnitureType furnidataType = entries.get(0).type();

                if (furnidataType != null && item.getType() != null && furnidataType != item.getType()) {
                    findings.add(new Finding("TYPE_MISMATCH", item.getId(), classname, item.getSpriteId(),
                            "items_base dice " + item.getType() + ", furnidata dice " + furnidataType));
                }
            }

            if (furnitureDirectory != null) {
                String bundle = bundleName(classname);

                if (!bundle.isEmpty() && bundlesChecked.add(bundle)
                        && !Files.isRegularFile(furnitureDirectory.resolve(bundle + ".nitro"))) {
                    findings.add(new Finding("MISSING_BUNDLE", item.getId(), classname, item.getSpriteId(),
                            bundle + ".nitro non esiste: il client disegna un placeholder"));
                }
            }

            List<Item> sharing = bySprite.getOrDefault(item.getSpriteId(), List.of());

            if (sharing.size() > 1) {
                findings.add(new Finding("DUPLICATE_SPRITE_ID", item.getId(), classname, item.getSpriteId(),
                        "sprite id condiviso con " + sharing.stream()
                                .filter(other -> other.getId() != item.getId())
                                .map(other -> other.getId() + "/" + other.getName())
                                .toList()));
            }

            if (classname.endsWith("_name") || classname.endsWith("_desc")) {
                findings.add(new Finding("GHOST_CLASSNAME", item.getId(), classname, item.getSpriteId(),
                        "classname preso da una chiave di localizzazione, non dal bundle"));
            }

            String publicName = item.getFullName() == null ? "" : item.getFullName().trim();

            if (publicName.isEmpty() || publicName.equals(classname) || publicName.endsWith("_name")) {
                findings.add(new Finding("PLACEHOLDER_NAME", item.getId(), classname, item.getSpriteId(),
                        "public_name segnaposto: \"" + publicName + "\""));
            }

            String rawType = rawTypes.get(item.getId());

            if (rawType != null && !VALID_TYPES.contains(rawType.toLowerCase(Locale.ROOT))) {
                findings.add(new Finding("BAD_TYPE", item.getId(), classname, item.getSpriteId(),
                        "items_base.type = \"" + rawType + "\", non e' ne' 's' ne' 'i': caricato come pavimento"));
            }

            if (isWiredBox(item) && !item.allowWalk()) {
                findings.add(new Finding("WIRED_BLOCKS_TILE", item.getId(), classname, item.getSpriteId(),
                        "wired con allow_walk=0: blocca la casella anche quando e' nascosto"));
            }
        }

        findings.sort(Comparator
                .comparingInt((Finding finding) -> CODES.indexOf(finding.code()))
                .thenComparing(Finding::classname));

        return findings;
    }

    /**
     * Only floor and wall furni are drawn from furnidata and a bundle. items_base also holds badges,
     * effects, bots, HC and pet rows, which have neither by design and would otherwise fill the
     * report with false placeholders.
     */
    private static boolean isRoomFurni(Item item) {
        if (item.getType() != FurnitureType.FLOOR && item.getType() != FurnitureType.WALL) {
            return false;
        }

        return item.getName() == null || !PET_BASE.matcher(item.getName()).matches();
    }

    /** Wired boxes are walkable furni in every official set; the config and display ones are not. */
    private static boolean isWiredBox(Item item) {
        if (item.getInteractionType() == null || item.getInteractionType().getName() == null) {
            return false;
        }

        String interaction = item.getInteractionType().getName().toLowerCase(Locale.ROOT);

        return interaction.startsWith("wf_act_")
                || interaction.startsWith("wf_trg_")
                || interaction.startsWith("wf_cnd_")
                || interaction.startsWith("wf_slc_")
                || interaction.startsWith("wf_xtra_")
                || interaction.startsWith("wf_var_");
    }

    /** The renderer strips a {@code *N} colour suffix before it loads the bundle. */
    private static String bundleName(String classname) {
        int star = classname.indexOf('*');

        return star < 0 ? classname : classname.substring(0, star);
    }

    // ------------------------------------------------------------------ the sources

    private static Map<Integer, Item> allBaseItems() {
        Int2ObjectMap<Item> items = Emulator.getGameEnvironment().getItemManager().getItems();
        Map<Integer, Item> out = new LinkedHashMap<>();

        for (Int2ObjectMap.Entry<Item> entry : items.int2ObjectEntrySet()) {
            out.put(entry.getIntKey(), entry.getValue());
        }

        return out;
    }

    private static Map<Integer, Item> baseItemsInRoom(Habbo habbo) {
        Room room = habbo.getHabboInfo().getCurrentRoom();
        Map<Integer, Item> out = new LinkedHashMap<>();

        if (room == null) {
            return out;
        }

        for (HabboItem item : room.getFloorItems()) {
            if (item.getBaseItem() != null) {
                out.put(item.getBaseItem().getId(), item.getBaseItem());
            }
        }

        for (HabboItem item : room.getWallItems()) {
            if (item.getBaseItem() != null) {
                out.put(item.getBaseItem().getId(), item.getBaseItem());
            }
        }

        return out;
    }

    private static List<FurnidataEntry> readFurnidata() {
        FurnitureTextProvider provider = Emulator.getGameEnvironment() == null
                ? null
                : Emulator.getGameEnvironment().getFurnitureTextProvider();

        if (provider != null && provider.getSource() != null) {
            return new FurnidataReader(provider.getSource(), provider.getMaxBytes()).read();
        }

        FurnidataSourceResolver.Source source = FurnidataSourceResolver.resolve();

        if (source == null || !source.ok()) {
            return List.of();
        }

        return new FurnidataReader(source.path(), 256L * 1024L * 1024L).read();
    }

    /** {@code <asset base>/furniture}, or null when the emulator has no path to the client assets. */
    private static Path furnitureDirectory() {
        String base = Emulator.getConfig().getValue("furni.editor.asset.base.path", "");

        if (base.isEmpty()) {
            return null;
        }

        Path directory = Paths.get(base, "furniture");

        return Files.isDirectory(directory) ? directory : null;
    }

    /**
     * The raw {@code type} column. FurnitureType.fromString() quietly turns anything it does not
     * recognise into FLOOR, so a broken type is invisible once the row is loaded.
     */
    private static Map<Integer, String> rawTypes() {
        Map<Integer, String> types = new HashMap<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT id, type FROM items_base");
                ResultSet set = statement.executeQuery()) {
            while (set.next()) {
                types.put(set.getInt("id"), set.getString("type"));
            }
        } catch (Exception exception) {
            LOGGER.error("Furni validation could not read items_base.type", exception);
        }

        return types;
    }

    // ------------------------------------------------------------------ the report

    private void report(
            Habbo habbo,
            List<Finding> findings,
            int checked,
            int furnidataEntries,
            Path furnitureDirectory,
            boolean everything) {
        Map<String, Integer> counts = new TreeMap<>();

        for (Finding finding : findings) {
            counts.merge(finding.code(), 1, Integer::sum);
        }

        StringBuilder chat = new StringBuilder("<b>Validazione furni</b> (")
                .append(everything ? "tutto l'hotel" : "questa stanza")
                .append(")\r")
                .append("- furni controllati: ").append(checked).append("\r")
                .append("- voci furnidata: ").append(furnidataEntries).append("\r");

        if (furnitureDirectory == null) {
            chat.append("- bundle: NON controllati (furni.editor.asset.base.path non punta ai file)\r");
        }

        if (findings.isEmpty()) {
            chat.append("\rNessun problema: nessun furni verra' disegnato come placeholder.");
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
                    .append(finding.classname()).append(" (base ").append(finding.baseId())
                    .append(", sprite ").append(finding.spriteId()).append("): ")
                    .append(finding.detail()).append("\r");
        }

        if (findings.size() > WHISPERED_DETAILS) {
            chat.append("... e altri ").append(findings.size() - WHISPERED_DETAILS).append("\r");
        }

        Path written = writeReport(findings, checked, furnidataEntries, everything);

        if (written != null) {
            chat.append("\rReport completo: ").append(written.toAbsolutePath());
        }

        habbo.whisper(chat.toString());
    }

    private Path writeReport(List<Finding> findings, int checked, int furnidataEntries, boolean everything) {
        try {
            Path directory = Paths.get("logging");
            Files.createDirectories(directory);

            Path file = directory.resolve("furni-validation-" + LocalDateTime.now().format(STAMP) + ".txt");
            StringBuilder out = new StringBuilder();
            out.append("Furni validation - ").append(everything ? "all" : "current room").append('\n')
                    .append("generated: ").append(LocalDateTime.now()).append('\n')
                    .append("base items checked: ").append(checked).append('\n')
                    .append("furnidata entries: ").append(furnidataEntries).append('\n')
                    .append("problems: ").append(findings.size()).append("\n\n");

            for (Finding finding : findings) {
                out.append(finding.code()).append('\t')
                        .append("base=").append(finding.baseId()).append('\t')
                        .append("sprite=").append(finding.spriteId()).append('\t')
                        .append(finding.classname()).append('\t')
                        .append(finding.detail()).append('\n');
            }

            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);

            return file;
        } catch (Exception exception) {
            LOGGER.error("Furni validation could not write its report", exception);

            return null;
        }
    }
}
