package com.eu.habbo.habbohotel.hotelview;

import com.eu.habbo.Emulator;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

@Slf4j
public class NewsList {
    private final ArrayList<NewsWidget> newsWidgets;

    public NewsList() {
        this.newsWidgets = new ArrayList<>();
        this.reload();
    }


    public void reload() {
        synchronized (this.newsWidgets) {
            this.newsWidgets.clear();
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM hotelview_news ORDER BY id DESC LIMIT 10")) {
                while (set.next()) {
                    this.newsWidgets.add(new NewsWidget(set));
                }
            } catch (SQLException e) {
                log.error("Caught SQL exception", e);
            }
        }
    }


    public ArrayList<NewsWidget> getNewsWidgets() {
        return this.newsWidgets;
    }
}