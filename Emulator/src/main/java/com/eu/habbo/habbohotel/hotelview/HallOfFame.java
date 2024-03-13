package com.eu.habbo.habbohotel.hotelview;

import com.eu.habbo.Emulator;
import gnu.trove.map.hash.THashMap;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class HallOfFame {
    private final THashMap<Integer, HallOfFameWinner> winners = new THashMap<>();
    private String competitionName;
    public HallOfFame() {
        this.setCompetitionName("xmasRoomComp");

        this.reload();
    }

    public void reload() {
        this.winners.clear();

        synchronized (this.winners) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery(Emulator.getConfig().getValue("hotelview.halloffame.query"))) {
                while (set.next()) {
                    HallOfFameWinner winner = new HallOfFameWinner(set);
                    this.winners.put(winner.getId(), winner);
                }
            } catch (SQLException e) {
                log.error("Caught SQL exception", e);
            }
        }
    }

    public THashMap<Integer, HallOfFameWinner> getWinners() {
        return this.winners;
    }

    public String getCompetitionName() {
        return this.competitionName;
    }

    void setCompetitionName(String name) {
        this.competitionName = name;
    }
}
