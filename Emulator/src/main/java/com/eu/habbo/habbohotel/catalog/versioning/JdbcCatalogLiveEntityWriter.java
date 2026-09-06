package com.eu.habbo.habbohotel.catalog.versioning;

import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

/** Writes one catalog entity without rebuilding unrelated live catalog rows. */
public final class JdbcCatalogLiveEntityWriter implements CatalogLiveEntityWriter {
    static final String UPSERT_NORMAL_PAGE_SQL = "INSERT INTO catalog_pages "
            + "(id,parent_id,caption_save,caption,page_layout,icon_color,icon_image,min_rank,order_num,visible,enabled,"
            + "club_only,catalog_mode,vip_only,page_headline,page_teaser,page_special,page_text1,page_text2,"
            + "page_text_details,page_text_teaser,room_id,includes) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),caption_save=VALUES(caption_save),"
            + "caption=VALUES(caption),page_layout=VALUES(page_layout),icon_color=VALUES(icon_color),"
            + "icon_image=VALUES(icon_image),min_rank=VALUES(min_rank),order_num=VALUES(order_num),"
            + "visible=VALUES(visible),enabled=VALUES(enabled),club_only=VALUES(club_only),"
            + "catalog_mode=VALUES(catalog_mode),vip_only=VALUES(vip_only),page_headline=VALUES(page_headline),"
            + "page_teaser=VALUES(page_teaser),page_special=VALUES(page_special),page_text1=VALUES(page_text1),"
            + "page_text2=VALUES(page_text2),page_text_details=VALUES(page_text_details),"
            + "page_text_teaser=VALUES(page_text_teaser),room_id=VALUES(room_id),includes=VALUES(includes)";
    static final String UPSERT_BUILDER_PAGE_SQL = "INSERT INTO catalog_pages_bc "
            + "(id,parent_id,caption,page_layout,icon_color,icon_image,order_num,visible,enabled,page_headline,"
            + "page_teaser,page_special,page_text1,page_text2,page_text_details,page_text_teaser) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),"
            + "caption=VALUES(caption),page_layout=VALUES(page_layout),icon_color=VALUES(icon_color),"
            + "icon_image=VALUES(icon_image),order_num=VALUES(order_num),visible=VALUES(visible),"
            + "enabled=VALUES(enabled),page_headline=VALUES(page_headline),page_teaser=VALUES(page_teaser),"
            + "page_special=VALUES(page_special),page_text1=VALUES(page_text1),page_text2=VALUES(page_text2),"
            + "page_text_details=VALUES(page_text_details),page_text_teaser=VALUES(page_text_teaser)";
    static final String UPSERT_NORMAL_OFFER_SQL = "INSERT INTO catalog_items "
            + "(id,item_ids,page_id,catalog_name,cost_credits,cost_points,points_type,amount,limited_stack,"
            + "order_number,offer_id,song_id,extradata,have_offer,club_only) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE item_ids=VALUES(item_ids),page_id=VALUES(page_id),"
            + "catalog_name=VALUES(catalog_name),cost_credits=VALUES(cost_credits),cost_points=VALUES(cost_points),"
            + "points_type=VALUES(points_type),amount=VALUES(amount),limited_stack=VALUES(limited_stack),"
            + "order_number=VALUES(order_number),offer_id=VALUES(offer_id),song_id=VALUES(song_id),"
            + "extradata=VALUES(extradata),have_offer=VALUES(have_offer),club_only=VALUES(club_only)";
    static final String UPSERT_BUILDER_OFFER_SQL = "INSERT INTO catalog_items_bc "
            + "(id,item_ids,page_id,catalog_name,order_number,extradata) VALUES (?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE item_ids=VALUES(item_ids),page_id=VALUES(page_id),"
            + "catalog_name=VALUES(catalog_name),order_number=VALUES(order_number),extradata=VALUES(extradata)";

    private final Gson gson;

    public JdbcCatalogLiveEntityWriter(Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    @Override
    public void apply(Connection connection, CatalogChangeEntry change) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(change, "change");
        if (change.afterJson() == null) {
            delete(connection, change);
            return;
        }
        if (change.entityType() == CatalogEntityType.OFFER) {
            CatalogOfferSnapshot offer = gson.fromJson(change.afterJson(), CatalogOfferSnapshot.class);
            if (offer.offerId() != change.entityId() || offer.catalogType() != change.catalogType()) {
                throw new IllegalArgumentException("Offer JSON identity does not match the live change");
            }
            boolean strictInsert = change.operation() == CatalogChangeOperation.CREATE;
            if (offer.catalogType() == CatalogPageType.BUILDER) upsertBuilderOffer(connection, offer, strictInsert);
            else upsertNormalOffer(connection, offer, strictInsert);
            return;
        }
        CatalogPageSnapshot page = gson.fromJson(change.afterJson(), CatalogPageSnapshot.class);
        if (page.pageId() != change.entityId() || page.catalogType() != change.catalogType()) {
            throw new IllegalArgumentException("Page JSON identity does not match the live change");
        }
        boolean strictInsert = change.operation() == CatalogChangeOperation.CREATE;
        if (page.catalogType() == CatalogPageType.BUILDER) upsertBuilderPage(connection, page, strictInsert);
        else upsertNormalPage(connection, page, strictInsert);
    }

    /** CREATE must never touch an existing row: drop the ON DUPLICATE KEY UPDATE tail so a collision throws. */
    static String insertSql(String upsertSql, boolean strictInsert) {
        if (!strictInsert) return upsertSql;
        int index = upsertSql.indexOf(" ON DUPLICATE KEY UPDATE");
        return index < 0 ? upsertSql : upsertSql.substring(0, index);
    }

    private static void delete(Connection connection, CatalogChangeEntry change) throws SQLException {
        String table =
                switch (change.entityType()) {
                    case PAGE -> change.catalogType() == CatalogPageType.BUILDER ? "catalog_pages_bc" : "catalog_pages";
                    case OFFER ->
                        change.catalogType() == CatalogPageType.BUILDER ? "catalog_items_bc" : "catalog_items";
                };
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
            statement.setInt(1, change.entityId());
            if (statement.executeUpdate() != 1)
                throw new SQLException("Live catalog entity disappeared before deletion");
        }
    }

    private static void upsertNormalPage(Connection connection, CatalogPageSnapshot page, boolean strictInsert) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(insertSql(UPSERT_NORMAL_PAGE_SQL, strictInsert))) {
            statement.setInt(1, page.pageId());
            statement.setInt(2, page.parentId());
            statement.setString(3, page.captionSave());
            statement.setString(4, page.caption());
            statement.setString(5, page.pageLayout());
            statement.setInt(6, page.iconColor());
            statement.setInt(7, page.iconImage());
            statement.setInt(8, page.minRank());
            statement.setInt(9, page.orderNum());
            // The live catalog schema stores flags as ENUM('0','1'). Binding
            // them as JDBC booleans makes MariaDB 12 strict mode receive an
            // invalid enum value and abort the mutation.
            statement.setString(10, enumFlag(page.visible()));
            statement.setString(11, enumFlag(page.enabled()));
            statement.setString(12, page.clubOnly() ? "1" : "0");
            statement.setString(13, page.catalogMode());
            statement.setString(14, enumFlag(page.vipOnly()));
            statement.setString(15, page.pageHeadline());
            statement.setString(16, page.pageTeaser());
            statement.setString(17, page.pageSpecial());
            statement.setString(18, page.pageText1());
            statement.setString(19, page.pageText2());
            statement.setString(20, page.pageTextDetails());
            statement.setString(21, page.pageTextTeaser());
            statement.setInt(22, page.roomId());
            statement.setString(23, page.includes());
            if (statement.executeUpdate() == 0) throw new SQLException("Live catalog page upsert changed no row");
        }
    }

    private static void upsertBuilderPage(Connection connection, CatalogPageSnapshot page, boolean strictInsert) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(insertSql(UPSERT_BUILDER_PAGE_SQL, strictInsert))) {
            statement.setInt(1, page.pageId());
            statement.setInt(2, page.parentId());
            statement.setString(3, page.caption());
            statement.setString(4, page.pageLayout());
            statement.setInt(5, page.iconColor());
            statement.setInt(6, page.iconImage());
            statement.setInt(7, page.orderNum());
            statement.setString(8, enumFlag(page.visible()));
            statement.setString(9, enumFlag(page.enabled()));
            statement.setString(10, page.pageHeadline());
            statement.setString(11, page.pageTeaser());
            statement.setString(12, page.pageSpecial());
            statement.setString(13, page.pageText1());
            statement.setString(14, page.pageText2());
            statement.setString(15, page.pageTextDetails());
            statement.setString(16, page.pageTextTeaser());
            if (statement.executeUpdate() == 0) throw new SQLException("Live builder page upsert changed no row");
        }
    }

    private static void upsertNormalOffer(Connection connection, CatalogOfferSnapshot offer, boolean strictInsert) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(insertSql(UPSERT_NORMAL_OFFER_SQL, strictInsert))) {
            statement.setInt(1, offer.offerId());
            statement.setString(2, offer.itemIds());
            statement.setInt(3, offer.pageId());
            statement.setString(4, offer.catalogName());
            statement.setInt(5, offer.costCredits());
            statement.setInt(6, offer.costPoints());
            statement.setInt(7, offer.pointsType());
            statement.setInt(8, offer.amount());
            statement.setInt(9, offer.limitedStack());
            statement.setInt(10, offer.orderNumber());
            statement.setInt(11, offer.offerIdClient());
            statement.setInt(12, offer.songId());
            statement.setString(13, offer.extradata());
            statement.setString(14, enumFlag(offer.haveOffer()));
            statement.setString(15, enumFlag(offer.clubOnly()));
            if (statement.executeUpdate() == 0) throw new SQLException("Live catalog offer upsert changed no row");
        }
    }

    private static void upsertBuilderOffer(Connection connection, CatalogOfferSnapshot offer, boolean strictInsert) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(insertSql(UPSERT_BUILDER_OFFER_SQL, strictInsert))) {
            statement.setInt(1, offer.offerId());
            statement.setString(2, offer.itemIds());
            statement.setInt(3, offer.pageId());
            statement.setString(4, offer.catalogName());
            statement.setInt(5, offer.orderNumber());
            statement.setString(6, offer.extradata());
            if (statement.executeUpdate() == 0) throw new SQLException("Live builder offer upsert changed no row");
        }
    }

    private static String enumFlag(boolean value) {
        return value ? "1" : "0";
    }
}
