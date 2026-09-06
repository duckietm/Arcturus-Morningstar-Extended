package com.eu.habbo.habbohotel.users.clothingvalidation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class Figuredata {
    public Map<Integer, FiguredataPalette> palettes;
    public Map<String, FiguredataSettype> settypes;

    public Figuredata() {
        palettes = new TreeMap<>();
        settypes = new TreeMap<>();
    }

    /**
     * Loads the figure data used by the clothing validation from either the classic figuredata.xml or the
     * Nitro FigureData.json (the format is detected from the content, not from the extension), so the
     * emulator can point at the same file the client uses.
     *
     * @param uri http(s) URL, file: URI or local path
     */
    public void parseXML(String uri) throws Exception, ParserConfigurationException, IOException, SAXException {
        String content = stripBom(readContent(uri)).trim();

        if (content.startsWith("{")) {
            parseJson(content);
            return;
        }

        parseXmlContent(content);
    }

    private static String readContent(String uri) throws IOException {
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            try (InputStream input = URI.create(uri).toURL().openStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        Path path = uri.startsWith("file:") ? Path.of(URI.create(uri)) : Path.of(uri);

        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String stripBom(String content) {
        return content != null && content.startsWith("﻿") ? content.substring(1) : content == null ? "" : content;
    }

    private void parseJson(String content) throws Exception {
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        if (!root.has("palettes") || !root.get("palettes").isJsonArray() || !root.has("setTypes") || !root.get("setTypes").isJsonArray()) {
            throw new Exception("The passed file is not in Nitro FigureData format (palettes / setTypes missing).");
        }

        Map<Integer, FiguredataPalette> newPalettes = new TreeMap<>();
        Map<String, FiguredataSettype> newSettypes = new TreeMap<>();

        for (JsonElement paletteElement : root.getAsJsonArray("palettes")) {
            if (!paletteElement.isJsonObject()) continue;

            JsonObject paletteObject = paletteElement.getAsJsonObject();
            FiguredataPalette palette = new FiguredataPalette(intOf(paletteObject, "id", 0));

            for (JsonElement colorElement : arrayOf(paletteObject, "colors")) {
                if (!colorElement.isJsonObject()) continue;

                JsonObject color = colorElement.getAsJsonObject();
                palette.addColor(new FiguredataPaletteColor(
                        intOf(color, "id", 0),
                        intOf(color, "index", 0),
                        intOf(color, "club", 0) != 0,
                        boolOf(color, "selectable", true),
                        stringOf(color, "hexCode", "")));
            }

            newPalettes.put(palette.id, palette);
        }

        for (JsonElement setTypeElement : root.getAsJsonArray("setTypes")) {
            if (!setTypeElement.isJsonObject()) continue;

            JsonObject setTypeObject = setTypeElement.getAsJsonObject();
            FiguredataSettype settype = new FiguredataSettype(
                    stringOf(setTypeObject, "type", ""),
                    intOf(setTypeObject, "paletteId", 0),
                    boolOf(setTypeObject, "mandatory_m_0", false),
                    boolOf(setTypeObject, "mandatory_f_0", false),
                    boolOf(setTypeObject, "mandatory_m_1", false),
                    boolOf(setTypeObject, "mandatory_f_1", false));

            for (JsonElement setElement : arrayOf(setTypeObject, "sets")) {
                if (!setElement.isJsonObject()) continue;

                JsonObject set = setElement.getAsJsonObject();
                settype.addSet(new FiguredataSettypeSet(
                        intOf(set, "id", 0),
                        stringOf(set, "gender", "U"),
                        intOf(set, "club", 0) != 0,
                        boolOf(set, "colorable", false),
                        boolOf(set, "selectable", true),
                        boolOf(set, "preselectable", false),
                        boolOf(set, "sellable", false)));
            }

            newSettypes.put(settype.type, settype);
        }

        palettes.clear();
        palettes.putAll(newPalettes);
        settypes.clear();
        settypes.putAll(newSettypes);
    }

    private static JsonArray arrayOf(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static int intOf(JsonObject object, String key, int fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) return fallback;

        JsonPrimitive primitive = object.getAsJsonPrimitive(key);

        if (primitive.isBoolean()) return primitive.getAsBoolean() ? 1 : 0;

        try {
            return primitive.isNumber() ? primitive.getAsInt() : Integer.parseInt(primitive.getAsString().trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean boolOf(JsonObject object, String key, boolean fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) return fallback;

        JsonPrimitive primitive = object.getAsJsonPrimitive(key);

        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) return primitive.getAsInt() != 0;

        String value = primitive.getAsString().trim();

        return value.equals("1") || value.equalsIgnoreCase("true");
    }

    private static String stringOf(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private void parseXmlContent(String content) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(content)));

        Element rootElement = document.getDocumentElement();
        if (!rootElement.getTagName().equalsIgnoreCase("figuredata") || document.getElementsByTagName("colors").getLength() == 0 || document.getElementsByTagName("sets").getLength() == 0) {
            throw new Exception("The passed file is not in figuredata format. Received " + content.substring(0, Math.min(content.length(), 200)));
        }

        NodeList palettesList = document.getElementsByTagName("colors").item(0).getChildNodes();
        NodeList settypesList = document.getElementsByTagName("sets").item(0).getChildNodes();

        palettes.clear();
        settypes.clear();

        for (int i = 0; i < palettesList.getLength(); i++) {
            Node nNode = palettesList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) nNode;
                int paletteId = Integer.parseInt(element.getAttribute("id"));
                FiguredataPalette palette = new FiguredataPalette(paletteId);
                NodeList colorsList = nNode.getChildNodes();
                for (int ii = 0; ii < colorsList.getLength(); ii++) {
                    if (colorsList.item(ii).getNodeType() == Node.ELEMENT_NODE) {
                        Element colorElement = (Element) colorsList.item(ii);
                        FiguredataPaletteColor color = new FiguredataPaletteColor(
                                Integer.parseInt(colorElement.getAttribute("id")),
                                Integer.parseInt(colorElement.getAttribute("index")),
                                !colorElement.getAttribute("club").equals("0"),
                                colorElement.getAttribute("selectable").equals("1"),
                                colorElement.getTextContent()
                        );
                        palette.addColor(color);
                    }
                }
                palettes.put(palette.id, palette);
            }
        }

        for (int i = 0; i < settypesList.getLength(); i++) {
            Node nNode = settypesList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) nNode;
                String type = element.getAttribute("type");
                int paletteId = Integer.parseInt(element.getAttribute("paletteid"));
                boolean mandM0 = element.getAttribute("mand_m_0").equals("1");
                boolean mandF0 = element.getAttribute("mand_f_0").equals("1");
                boolean mandM1 = element.getAttribute("mand_m_1").equals("1");
                boolean mandF1 = element.getAttribute("mand_f_1").equals("1");
                FiguredataSettype settype = new FiguredataSettype(type, paletteId, mandM0, mandF0, mandM1, mandF1);
                NodeList setsList = nNode.getChildNodes();
                for (int ii = 0; ii < setsList.getLength(); ii++) {
                    if (setsList.item(ii).getNodeType() == Node.ELEMENT_NODE) {
                        Element setElement = (Element) setsList.item(ii);
                        FiguredataSettypeSet set = new FiguredataSettypeSet(
                                Integer.parseInt(setElement.getAttribute("id")),
                                setElement.getAttribute("gender"),
                                !setElement.getAttribute("club").equals("0"),
                                setElement.getAttribute("colorable").equals("1"),
                                setElement.getAttribute("selectable").equals("1"),
                                setElement.getAttribute("preselectable").equals("1"),
                                setElement.getAttribute("sellable").equals("1")
                        );
                        settype.addSet(set);
                    }
                }
                settypes.put(settype.type, settype);
            }
        }
    }
}
