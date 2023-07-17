package com.eu.habbo.habbohotel.hotelview;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HotelViewManager {
    private final HallOfFame hallOfFame;
    private final NewsList newsList;

    public HotelViewManager() {
        long millis = System.currentTimeMillis();
        this.hallOfFame = new HallOfFame();
        this.newsList = new NewsList();

        log.info("Hotelview Manager -> Loaded! ({} MS)", System.currentTimeMillis() - millis);
    }

    public HallOfFame getHallOfFame() {
        return this.hallOfFame;
    }

    public NewsList getNewsList() {
        return this.newsList;
    }

    public void dispose() {
        log.info("HotelView Manager -> Disposed!");
    }

}
