package com.eu.habbo.habbohotel.items;

import com.eu.habbo.Emulator;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FurniDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FurniDataManager.class);
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private String furniDataPath;
    private final Object lock = new Object();

    public FurniDataManager() {
        this.resolveFurniDataPath();
    }

    private void resolveFurniDataPath() {
        String rendererConfigPath = Emulator.getConfig().getValue("furni.editor.renderer.config.path", "");
        String assetBasePath = Emulator.getConfig().getValue("furni.editor.asset.base.path", "");

        if (rendererConfigPath.isEmpty() || assetBasePath.isEmpty()) {
            LOGGER.warn("FurniDataManager: furni.editor.renderer.config.path or furni.editor.asset.base.path not configured");
            return;
        }

        try {
            Path configFile = Paths.get(rendererConfigPath);
            if (!Files.exists(configFile)) {
                LOGGER.error("FurniDataManager: renderer-config.json not found at {}", rendererConfigPath);
                return;
            }

            String content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
            JsonObject config = JsonParser.parseString(content).getAsJsonObject();

            // Build variable map from config
            Properties vars = new Properties();
            for (String key : config.keySet()) {
                JsonElement el = config.get(key);
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    vars.setProperty(key, el.getAsString());
                }
            }

            // Resolve variables in furnidata.url
            String furniDataUrl = resolveVars(vars.getProperty("furnidata.url", ""), vars);

            if (furniDataUrl.isEmpty()) {
                LOGGER.error("FurniDataManager: could not resolve furnidata.url from renderer-config.json");
                return;
            }

            // Convert URL to filesystem path using asset.base.path
            String assetUrl = resolveVars(vars.getProperty("asset.url", ""), vars);
            if (!assetUrl.isEmpty() && furniDataUrl.startsWith(assetUrl)) {
                String relativePath = furniDataUrl.substring(assetUrl.length());
                this.furniDataPath = assetBasePath + relativePath;
            } else {
                // Fallback: try to extract path from URL
                this.furniDataPath = assetBasePath + "/gamedata/FurnitureData.json";
            }

            // Normalize path separators
            this.furniDataPath = this.furniDataPath.replace("/", File.separator).replace("\\", File.separator);

            if (Files.exists(Paths.get(this.furniDataPath))) {
                LOGGER.info("FurniDataManager: FurnitureData.json found at {}", this.furniDataPath);
            } else {
                LOGGER.warn("FurniDataManager: FurnitureData.json NOT found at {}", this.furniDataPath);
            }
        } catch (Exception e) {
            LOGGER.error("FurniDataManager: failed to resolve FurnitureData path", e);
        }
    }

    private String resolveVars(String value, Properties vars) {
        if (value == null) return "";
        String result = value;
        int maxIterations = 10;
        while (maxIterations-- > 0) {
            Matcher m = VAR_PATTERN.matcher(result);
            if (!m.find()) break;
            StringBuffer sb = new StringBuffer();
            m.reset();
            while (m.find()) {
                String varName = m.group(1);
                // Map dot notation: "gamedata.url" -> key in config
                String replacement = vars.getProperty(varName, "");
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    public boolean isAvailable() {
        return this.furniDataPath != null && !this.furniDataPath.isEmpty();
    }

    public JsonObject readFurniData() {
        if (!isAvailable()) return null;

        synchronized (this.lock) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(this.furniDataPath)), StandardCharsets.UTF_8);
                return JsonParser.parseString(content).getAsJsonObject();
            } catch (Exception e) {
                LOGGER.error("FurniDataManager: failed to read FurnitureData.json", e);
                return null;
            }
        }
    }

    public boolean writeFurniData(JsonObject data) {
        if (!isAvailable()) return false;

        synchronized (this.lock) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String json = gson.toJson(data);
                Files.write(Paths.get(this.furniDataPath), json.getBytes(StandardCharsets.UTF_8));
                return true;
            } catch (Exception e) {
                LOGGER.error("FurniDataManager: failed to write FurnitureData.json", e);
                return false;
            }
        }
    }

    public JsonObject findEntry(String classname) {
        JsonObject data = readFurniData();
        if (data == null) return null;

        // Search in roomitemtypes
        JsonObject entry = findInSection(data, "roomitemtypes", classname);
        if (entry != null) return entry;

        // Search in wallitemtypes
        return findInSection(data, "wallitemtypes", classname);
    }

    private JsonObject findInSection(JsonObject data, String section, String classname) {
        try {
            JsonObject sectionObj = data.getAsJsonObject(section);
            if (sectionObj == null) return null;

            JsonArray items = sectionObj.getAsJsonArray("furnitype");
            if (items == null) return null;

            for (JsonElement el : items) {
                JsonObject item = el.getAsJsonObject();
                if (classname.equals(item.get("classname").getAsString())) {
                    return item;
                }
            }
        } catch (Exception e) {
            LOGGER.error("FurniDataManager: error searching section {}", section, e);
        }
        return null;
    }

    public boolean updateEntry(String classname, String publicName) {
        if (!isAvailable()) return false;

        synchronized (this.lock) {
            try {
                JsonObject data = readUnsafe();
                if (data == null) return false;

                boolean updated = updateInSection(data, "roomitemtypes", classname, publicName);
                if (!updated) {
                    updated = updateInSection(data, "wallitemtypes", classname, publicName);
                }

                if (updated) {
                    return writeUnsafe(data);
                }
                return false;
            } catch (Exception e) {
                LOGGER.error("FurniDataManager: failed to update entry {}", classname, e);
                return false;
            }
        }
    }

    private boolean updateInSection(JsonObject data, String section, String classname, String publicName) {
        try {
            JsonObject sectionObj = data.getAsJsonObject(section);
            if (sectionObj == null) return false;

            JsonArray items = sectionObj.getAsJsonArray("furnitype");
            if (items == null) return false;

            for (JsonElement el : items) {
                JsonObject item = el.getAsJsonObject();
                if (classname.equals(item.get("classname").getAsString())) {
                    item.addProperty("name", publicName);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("FurniDataManager: error updating section {}", section, e);
        }
        return false;
    }

    public boolean removeEntry(String classname) {
        if (!isAvailable()) return false;

        synchronized (this.lock) {
            try {
                JsonObject data = readUnsafe();
                if (data == null) return false;

                boolean removed = removeFromSection(data, "roomitemtypes", classname);
                if (!removed) {
                    removed = removeFromSection(data, "wallitemtypes", classname);
                }

                if (removed) {
                    return writeUnsafe(data);
                }
                return false;
            } catch (Exception e) {
                LOGGER.error("FurniDataManager: failed to remove entry {}", classname, e);
                return false;
            }
        }
    }

    private boolean removeFromSection(JsonObject data, String section, String classname) {
        try {
            JsonObject sectionObj = data.getAsJsonObject(section);
            if (sectionObj == null) return false;

            JsonArray items = sectionObj.getAsJsonArray("furnitype");
            if (items == null) return false;

            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                if (classname.equals(item.get("classname").getAsString())) {
                    items.remove(i);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("FurniDataManager: error removing from section {}", section, e);
        }
        return false;
    }

    // Internal methods that don't acquire the lock (caller must hold it)
    private JsonObject readUnsafe() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(this.furniDataPath)), StandardCharsets.UTF_8);
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("FurniDataManager: failed to read FurnitureData.json", e);
            return null;
        }
    }

    private boolean writeUnsafe(JsonObject data) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String json = gson.toJson(data);
            Files.write(Paths.get(this.furniDataPath), json.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            LOGGER.error("FurniDataManager: failed to write FurnitureData.json", e);
            return false;
        }
    }

    public String getFurniDataPath() {
        return this.furniDataPath;
    }
}
