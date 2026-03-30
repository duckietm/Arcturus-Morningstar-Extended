package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages reading and writing of FurnitureData.json entries.
 * Resolves the file path from emulator config keys.
 */
public class FurniDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FurniDataManager.class);

    /**
     * Get the JSON string for a specific item from FurnitureData.json.
     * Returns "{}" if not found or on error.
     */
    public static String getItemJson(int itemId) {
        try {
            Path furniDataPath = resolveFurniDataPath();
            if (furniDataPath == null || !Files.exists(furniDataPath)) {
                return "{}";
            }

            String content = Files.readString(furniDataPath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            // Search in both "roomitemtypes" and "wallitemtypes"
            for (String section : new String[]{"roomitemtypes", "wallitemtypes"}) {
                if (!root.has(section)) continue;
                JsonObject sectionObj = root.getAsJsonObject(section);
                if (!sectionObj.has("furnitype")) continue;
                JsonArray types = sectionObj.getAsJsonArray("furnitype");

                for (JsonElement el : types) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("id") && obj.get("id").getAsInt() == itemId) {
                        return obj.toString();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read FurnitureData.json for item " + itemId, e);
        }

        return "{}";
    }

    /**
     * Resolve the path to FurnitureData.json from emulator config.
     */
    private static Path resolveFurniDataPath() {
        try {
            String configPath = Emulator.getConfig().getValue("furni.editor.renderer.config.path", "");

            if (configPath.isEmpty()) {
                // Fallback: try common locations
                String basePath = Emulator.getConfig().getValue("furni.editor.asset.base.path", "");
                if (!basePath.isEmpty()) {
                    Path candidate = Paths.get(basePath, "FurnitureData.json");
                    if (Files.exists(candidate)) return candidate;
                }
                return null;
            }

            // Read the renderer config to find the furnidata URL/path
            Path rendererConfig = Paths.get(configPath);
            if (!Files.exists(rendererConfig)) return null;

            String rendererContent = Files.readString(rendererConfig, StandardCharsets.UTF_8);
            JsonObject rendererObj = JsonParser.parseString(rendererContent).getAsJsonObject();

            if (rendererObj.has("furnidata.url")) {
                String furniUrl = rendererObj.get("furnidata.url").getAsString();

                // Skip unresolved placeholders like ${gamedata.url}
                if (furniUrl.contains("${")) {
                    String basePath = Emulator.getConfig().getValue("furni.editor.asset.base.path", "");
                    if (!basePath.isEmpty()) {
                        Path candidate = Paths.get(basePath, "FurnitureData.json");
                        if (Files.exists(candidate)) return candidate;
                    }
                    return null;
                }

                // Strip query string (?v=1 etc.)
                String cleanUrl = furniUrl.contains("?") ? furniUrl.substring(0, furniUrl.indexOf('?')) : furniUrl;

                // If it's a local file path (not http), use it directly
                if (!cleanUrl.startsWith("http")) {
                    return Paths.get(cleanUrl);
                }

                // For http URLs, try to derive local path from base path
                String basePath = Emulator.getConfig().getValue("furni.editor.asset.base.path", "");
                if (!basePath.isEmpty()) {
                    // Extract filename from URL (without query string)
                    String filename = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
                    return Paths.get(basePath, filename);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve FurnitureData.json path", e);
        }

        return null;
    }
}
