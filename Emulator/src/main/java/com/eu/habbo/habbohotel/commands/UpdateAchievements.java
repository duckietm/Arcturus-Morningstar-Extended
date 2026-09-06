package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.Achievement;
import com.eu.habbo.habbohotel.achievements.AchievementLevel;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.generic.alerts.MessagesForYouComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryAchievementsComposer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;

public class UpdateAchievements extends Command {
    public UpdateAchievements() {
        super("cmd_update_achievements", Emulator.getTexts().getValue("commands.keys.cmd_update_achievements").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params[0].equalsIgnoreCase("achievement") || params[0].equalsIgnoreCase("achievements")) {
            return handleAchievementTools(gameClient, params);
        }
        Emulator.getGameEnvironment().getAchievementManager().reload();
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_achievements.updated"), RoomChatMessageBubbles.ALERT);
        return true;
    }

    private boolean handleAchievementTools(GameClient client, String[] params) {
        if (params.length == 2 && params[1].equalsIgnoreCase("list")) {
            StringBuilder output = new StringBuilder("<b>Achievements disponibili</b>\r\n\r\n");
            Emulator.getGameEnvironment().getAchievementManager().getAchievements().values().stream()
                    .sorted(Comparator.comparing(achievement -> achievement.name))
                    .forEach(achievement -> output.append(achievement.name)
                            .append(" — livelli: ").append(achievement.levels.size()).append("\r\n"));
            client.sendResponse(new MessagesForYouComposer(new String[] {output.toString()}));
            return true;
        }

        if (params.length < 4 || !params[1].equalsIgnoreCase("set")) {
            client.getHabbo().whisper(":achievements list | :achievement set <utente> <achievement> <livello> | :achievement set <utente> maxall", RoomChatMessageBubbles.ALERT);
            return true;
        }

        Habbo target = Emulator.getGameEnvironment().getHabboManager().getHabbo(params[2]);
        if (target == null) {
            client.getHabbo().whisper("Utente non trovato.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (params[3].equalsIgnoreCase("maxall") && params.length == 4) {
            int changed = 0;
            for (Achievement achievement : Emulator.getGameEnvironment().getAchievementManager().getAchievements().values()) {
                AchievementLevel max = achievement.levels.values().stream().max(Comparator.comparingInt(level -> level.level)).orElse(null);
                if (max != null) {
                    setProgress(target, achievement, max.progress);
                    changed++;
                }
            }
            client.getHabbo().whisper("Achievement massimizzati per " + target.getHabboInfo().getUsername() + ": " + changed + ".", RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (params.length != 5) {
            client.getHabbo().whisper("Indica anche il livello dell'achievement.", RoomChatMessageBubbles.ALERT);
            return true;
        }
        Achievement achievement = Emulator.getGameEnvironment().getAchievementManager().getAchievement(params[3]);
        if (achievement == null) {
            client.getHabbo().whisper("Achievement non trovato. Usa :achievements list.", RoomChatMessageBubbles.ALERT);
            return true;
        }
        try {
            int level = Integer.parseInt(params[4]);
            AchievementLevel targetLevel = achievement.levels.get(level);
            if (targetLevel == null) throw new NumberFormatException();
            setProgress(target, achievement, targetLevel.progress);
            client.getHabbo().whisper("Achievement aggiornato.", RoomChatMessageBubbles.ALERT);
        } catch (NumberFormatException ignored) {
            client.getHabbo().whisper("Livello achievement non valido.", RoomChatMessageBubbles.ALERT);
        }
        return true;
    }

    private static void setProgress(Habbo target, Achievement achievement, int progress) {
        target.getHabboStats().setProgress(achievement, progress);
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users_achievements (user_id, achievement_name, progress) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE progress=VALUES(progress)")) {
            statement.setInt(1, target.getHabboInfo().getId());
            statement.setString(2, achievement.name);
            statement.setInt(3, progress);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save achievement progress", exception);
        }
        if (target.getClient() != null) target.getClient().sendResponse(new InventoryAchievementsComposer());
    }
}
