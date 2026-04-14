package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraTextInputVariable;
import com.eu.habbo.habbohotel.items.interactions.wired.triggers.WiredTriggerHabboSaysKeyword;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.api.WiredStack;
import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WiredTextInputCaptureSupport {
    private static final int MATCH_CONTAINS = 0;
    private static final int MATCH_EXACT = 1;
    private static final int MATCH_ALL_WORDS = 2;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("#([^#]+)#");

    private WiredTextInputCaptureSupport() {
    }

    public static CaptureResult resolve(WiredStack stack, WiredEvent event) {
        if (stack == null || event == null || !(stack.triggerItem() instanceof WiredTriggerHabboSaysKeyword)) {
            return CaptureResult.noMatch();
        }

        WiredTriggerHabboSaysKeyword trigger = (WiredTriggerHabboSaysKeyword) stack.triggerItem();
        Room room = event.getRoom();
        RoomUnit actor = event.getActor().orElse(null);
        String text = event.getText().orElse(null);

        if (room == null || actor == null || text == null) {
            return CaptureResult.noMatch();
        }

        List<WiredExtraTextInputVariable> capturers = getCapturers(room, trigger);
        if (capturers.isEmpty()) {
            return trigger.matches(stack.triggerItem(), event) ? CaptureResult.matched(new LinkedHashMap<>()) : CaptureResult.noMatch();
        }

        if (trigger.isOwnerOnly()) {
            Habbo habbo = room.getHabbo(actor);
            if (habbo == null || room.getOwnerId() != habbo.getHabboInfo().getId()) {
                return CaptureResult.noMatch();
            }
        }

        LinkedHashMap<String, WiredExtraTextInputVariable> capturersByName = new LinkedHashMap<>();
        for (WiredExtraTextInputVariable capturer : capturers) {
            if (capturer == null || capturer.getCapturerName() == null || capturer.getCapturerName().trim().isEmpty()) {
                continue;
            }

            capturersByName.put(capturer.getCapturerName().trim().toLowerCase(), capturer);
        }

        if (capturersByName.isEmpty()) {
            return trigger.matches(stack.triggerItem(), event) ? CaptureResult.matched(new LinkedHashMap<>()) : CaptureResult.noMatch();
        }

        MatchResult matchResult = matchTemplate(trigger, text, capturersByName);
        if (!matchResult.matches) {
            return CaptureResult.noMatch();
        }

        LinkedHashMap<Integer, Integer> capturedValues = new LinkedHashMap<>();
        for (Map.Entry<String, String> capture : matchResult.captures.entrySet()) {
            WiredExtraTextInputVariable capturer = capturersByName.get(capture.getKey());
            if (capturer == null) {
                continue;
            }

            Integer resolvedValue = capturer.resolveCapturedValue(room, capture.getValue());
            if (resolvedValue == null) {
                return CaptureResult.noMatch();
            }

            capturedValues.put(capturer.getVariableItemId(), resolvedValue);
        }

        return CaptureResult.matched(capturedValues);
    }

    private static List<WiredExtraTextInputVariable> getCapturers(Room room, WiredTriggerHabboSaysKeyword trigger) {
        List<WiredExtraTextInputVariable> capturers = new ArrayList<>();

        if (room == null || trigger == null || room.getRoomSpecialTypes() == null) {
            return capturers;
        }

        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(trigger.getX(), trigger.getY());
        if (extras == null || extras.isEmpty()) {
            return capturers;
        }

        for (InteractionWiredExtra extra : WiredExecutionOrderUtil.sort(extras)) {
            if (extra instanceof WiredExtraTextInputVariable) {
                capturers.add((WiredExtraTextInputVariable) extra);
            }
        }

        return capturers;
    }

    private static MatchResult matchTemplate(WiredTriggerHabboSaysKeyword trigger, String rawText, Map<String, WiredExtraTextInputVariable> capturersByName) {
        String text = rawText != null ? rawText.trim() : "";
        String template = trigger.getKey() != null ? trigger.getKey().trim() : "";

        if (trigger.getMatchMode() == MATCH_ALL_WORDS && template.isEmpty()) {
            if (capturersByName.size() != 1 || text.isEmpty()) {
                return MatchResult.noMatch();
            }

            String placeholderName = capturersByName.keySet().iterator().next();
            LinkedHashMap<String, String> captures = new LinkedHashMap<>();
            captures.put(placeholderName, text);
            return MatchResult.matched(captures);
        }

        TemplatePattern pattern = buildPattern(template);
        if (pattern == null) {
            return MatchResult.noMatch();
        }

        Matcher matcher = pattern.pattern.matcher(text);
        boolean matches = (trigger.getMatchMode() == MATCH_CONTAINS) ? matcher.find() : matcher.matches();
        if (!matches) {
            return MatchResult.noMatch();
        }

        LinkedHashMap<String, String> captures = new LinkedHashMap<>();
        for (int index = 0; index < pattern.placeholderNames.size(); index++) {
            String placeholderName = pattern.placeholderNames.get(index);
            if (!capturersByName.containsKey(placeholderName)) {
                continue;
            }

            String capturedValue = matcher.group(index + 1);
            captures.put(placeholderName, capturedValue != null ? capturedValue.trim() : "");
        }

        return MatchResult.matched(captures);
    }

    private static TemplatePattern buildPattern(String template) {
        if (template == null || template.isEmpty()) {
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder regex = new StringBuilder();
        List<String> placeholderNames = new ArrayList<>();
        int cursor = 0;

        while (matcher.find()) {
            regex.append(Pattern.quote(template.substring(cursor, matcher.start())));
            regex.append("(.+?)");

            String placeholderName = matcher.group(1) != null ? matcher.group(1).trim().toLowerCase() : "";
            placeholderNames.add(placeholderName);
            cursor = matcher.end();
        }

        regex.append(Pattern.quote(template.substring(cursor)));

        if (placeholderNames.isEmpty()) {
            regex = new StringBuilder(Pattern.quote(template));
        }

        return new TemplatePattern(Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), placeholderNames);
    }

    public static void applyToContext(WiredContext ctx, Room room, CaptureResult captureResult) {
        if (ctx == null || room == null || captureResult == null || !captureResult.matches || captureResult.capturedValues.isEmpty()) {
            return;
        }

        ctx.forkContextVariables();

        for (Map.Entry<Integer, Integer> entry : captureResult.capturedValues.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey() <= 0) {
                continue;
            }

            if (!WiredContextVariableSupport.updateVariableValue(ctx, room, entry.getKey(), entry.getValue())) {
                WiredContextVariableSupport.assignVariable(ctx, room, entry.getKey(), entry.getValue(), false);
            }
        }
    }

    public static final class CaptureResult {
        private final boolean matches;
        private final LinkedHashMap<Integer, Integer> capturedValues;

        private CaptureResult(boolean matches, LinkedHashMap<Integer, Integer> capturedValues) {
            this.matches = matches;
            this.capturedValues = capturedValues;
        }

        public boolean matches() {
            return this.matches;
        }

        public static CaptureResult matched(LinkedHashMap<Integer, Integer> capturedValues) {
            return new CaptureResult(true, capturedValues);
        }

        public static CaptureResult noMatch() {
            return new CaptureResult(false, new LinkedHashMap<>());
        }
    }

    private static final class MatchResult {
        private final boolean matches;
        private final LinkedHashMap<String, String> captures;

        private MatchResult(boolean matches, LinkedHashMap<String, String> captures) {
            this.matches = matches;
            this.captures = captures;
        }

        private static MatchResult matched(LinkedHashMap<String, String> captures) {
            return new MatchResult(true, captures);
        }

        private static MatchResult noMatch() {
            return new MatchResult(false, new LinkedHashMap<>());
        }
    }

    private static final class TemplatePattern {
        private final Pattern pattern;
        private final List<String> placeholderNames;

        private TemplatePattern(Pattern pattern, List<String> placeholderNames) {
            this.pattern = pattern;
            this.placeholderNames = placeholderNames;
        }
    }
}
