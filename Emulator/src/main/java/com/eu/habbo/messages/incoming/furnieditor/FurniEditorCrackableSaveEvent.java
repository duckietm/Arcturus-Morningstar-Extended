package com.eu.habbo.messages.incoming.furnieditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.editor.FurniEditorRepository;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.furnieditor.FurniEditorResultComposer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Furni editor: create / update / delete the items_crackable row of a base item.
 *
 * <p>Payload (JSON string after the item id): {@code {"remove":true}} deletes the configuration; otherwise
 * {@code {"count":n,"achievementTick":"","achievementCracked":"","requiredEffect":0,"subscriptionDuration":0,
 * "subscriptionType":"","prizes":[{"itemId":123,"chance":50}, ...]}}. Chances are relative weights, exactly like the
 * {@code prizes} column ("itemId:chance;itemId:chance").
 */
public class FurniEditorCrackableSaveEvent extends MessageHandler {
    private static final int MAX_COUNT = 1_000_000_000;
    private static final int MAX_CHANCE = 1_000_000;
    private static final int MAX_PRIZES = 500;

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "No permission"));
            return;
        }

        int id = this.packet.readInt();
        String payload = this.packet.readString();

        if (id <= 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid item ID"));
            return;
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(payload).getAsJsonObject();
        } catch (Exception e) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Invalid JSON data"));
            return;
        }

        FurniEditorRepository repository = new FurniEditorRepository(Emulator.getDatabase().getDataSource());
        String classname = repository.findClassname(id).orElse(null);

        if (classname == null) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Item not found: " + id));
            return;
        }

        if (json.has("remove") && json.get("remove").isJsonPrimitive() && json.get("remove").getAsBoolean()) {
            boolean removed = repository.deleteCrackable(id);
            Emulator.getGameEnvironment().getItemManager().loadCrackable();
            this.client.sendResponse(new FurniEditorResultComposer(
                    true, removed ? "Configurazione crackable rimossa" : "Nessuna configurazione da rimuovere", id));
            FurniEditorCrackableEvent.sendCrackable(this.client, id);
            return;
        }

        int count = readInt(json, "count", -1);
        if (count < 1 || count > MAX_COUNT) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Numero di click non valido (1 - " + MAX_COUNT + ")"));
            return;
        }

        String achievementTick = readString(json, "achievementTick", 64);
        String achievementCracked = readString(json, "achievementCracked", 64);
        if (achievementTick == null || achievementCracked == null) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Nome achievement troppo lungo (max 64)"));
            return;
        }

        int requiredEffect = readInt(json, "requiredEffect", 0);
        int subscriptionDuration = readInt(json, "subscriptionDuration", 0);
        if (requiredEffect < 0 || subscriptionDuration < 0) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Effetto o durata abbonamento non validi"));
            return;
        }

        String subscriptionType = readString(json, "subscriptionType", 16);
        if (subscriptionType == null) subscriptionType = "";
        subscriptionType = subscriptionType.trim().toLowerCase();
        if (!subscriptionType.isEmpty() && !"hc".equals(subscriptionType) && !"bc".equals(subscriptionType)) {
            this.client.sendResponse(new FurniEditorResultComposer(false, "Tipo abbonamento non valido (vuoto, hc o bc)"));
            return;
        }

        Map<Integer, Integer> prizes = new LinkedHashMap<>();
        if (json.has("prizes") && json.get("prizes").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("prizes");
            if (array.size() > MAX_PRIZES) {
                this.client.sendResponse(new FurniEditorResultComposer(false, "Troppi premi (max " + MAX_PRIZES + ")"));
                return;
            }

            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject prize = element.getAsJsonObject();
                int itemId = readInt(prize, "itemId", 0);
                int chance = readInt(prize, "chance", 100);

                if (itemId <= 0 || chance < 1 || chance > MAX_CHANCE) {
                    this.client.sendResponse(new FurniEditorResultComposer(
                            false, "Premio non valido: id " + itemId + " probabilità " + chance));
                    return;
                }

                prizes.merge(itemId, chance, Integer::sum);
            }
        }

        if (!prizes.isEmpty()) {
            Set<Integer> existing = repository.findExistingItemIds(prizes.keySet());
            List<Integer> missing = new ArrayList<>();
            for (Integer prizeId : prizes.keySet()) if (!existing.contains(prizeId)) missing.add(prizeId);

            if (!missing.isEmpty()) {
                this.client.sendResponse(new FurniEditorResultComposer(false, "Premi inesistenti in items_base: " + missing));
                return;
            }
        }

        StringBuilder prizeList = new StringBuilder();
        for (Map.Entry<Integer, Integer> prize : prizes.entrySet()) {
            if (prizeList.length() > 0) prizeList.append(';');
            prizeList.append(prize.getKey()).append(':').append(prize.getValue());
        }

        repository.upsertCrackable(
                id,
                classname,
                count,
                prizeList.toString(),
                achievementTick.trim(),
                achievementCracked.trim(),
                requiredEffect,
                subscriptionDuration > 0 ? subscriptionDuration : null,
                subscriptionType.isEmpty() ? null : subscriptionType);

        Emulator.getGameEnvironment().getItemManager().loadCrackable();

        this.client.sendResponse(new FurniEditorResultComposer(
                true, "Crackable salvato · " + prizes.size() + (prizes.size() == 1 ? " premio" : " premi"), id));
        FurniEditorCrackableEvent.sendCrackable(this.client, id);
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        if (!json.has(key) || json.get(key).isJsonNull() || !json.get(key).isJsonPrimitive()) return fallback;
        try {
            return json.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    /** Returns the string (empty when absent) or null when it exceeds maxLength. */
    private static String readString(JsonObject json, String key, int maxLength) {
        if (!json.has(key) || json.get(key).isJsonNull() || !json.get(key).isJsonPrimitive()) return "";
        String value = json.get(key).getAsString();
        return value.length() > maxLength ? null : value;
    }
}
