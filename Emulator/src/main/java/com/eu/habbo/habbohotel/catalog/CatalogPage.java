package com.eu.habbo.habbohotel.catalog;

import com.eu.habbo.messages.ISerialize;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.TCollections;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public abstract class CatalogPage implements Comparable<CatalogPage>, ISerialize {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPage.class);

    protected final TIntArrayList offerIds = new TIntArrayList();
    protected final THashMap<Integer, CatalogPage> childPages = new THashMap<>();
    private final TIntObjectMap<CatalogItem> catalogItems = TCollections.synchronizedMap(new TIntObjectHashMap<>());
    private final ArrayList<Integer> included = new ArrayList<>();
    protected int id;
    protected int parentId;
    protected int rank;
    protected String caption;
    protected String pageName;
    protected int iconColor;
    protected int iconImage;
    protected int orderNum;
    protected boolean visible;
    protected boolean enabled;
    protected boolean clubOnly;
    protected String layout;
    protected String headerImage;
    protected String teaserImage;
    protected String specialImage;
    protected String textOne;
    protected String textTwo;
    protected String textDetails;
    protected String textTeaser;

    public CatalogPage() {
    }

    public CatalogPage(ResultSet set) throws SQLException {
        if (set == null)
            return;

        this.id = set.getInt("id");
        this.parentId = set.getInt("parent_id");
        this.rank = set.getInt("min_rank");
        this.caption = set.getString("caption");
        this.pageName = set.getString("caption_save");
        this.iconColor = set.getInt("icon_color");
        this.iconImage = set.getInt("icon_image");
        this.orderNum = set.getInt("order_num");
        this.visible = set.getBoolean("visible");
        this.enabled = set.getBoolean("enabled");
        this.clubOnly = set.getBoolean("club_only");
        this.layout = set.getString("page_layout");
        this.headerImage = set.getString("page_headline");
        this.teaserImage = set.getString("page_teaser");
        this.specialImage = set.getString("page_special");
        this.textOne = set.getString("page_text1");
        this.textTwo = set.getString("page_text2");
        this.textDetails = set.getString("page_text_details");
        this.textTeaser = set.getString("page_text_teaser");

        if (!set.getString("includes").isEmpty()) {
            for (String id : set.getString("includes").split(";")) {
                try {
                    this.included.add(Integer.valueOf(id));
                } catch (Exception e) {
                    LOGGER.error("Caught exception", e);
                    LOGGER.error("Failed to parse includes column value of (" + id + ") for catalog page (" + this.id + ")");
                }
            }
        }
    }

    public int getId() {
        return this.id;
    }

    public int getParentId() {
        return this.parentId;
    }

    public int getRank() {
        return this.rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getCaption() {
        return this.caption;
    }

    public String getPageName() {
        return this.pageName;
    }

    public int getIconColor() {
        return this.iconColor;
    }

    public int getIconImage() {
        return this.iconImage;
    }

    public int getOrderNum() {
        return this.orderNum;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isClubOnly() {
        return this.clubOnly;
    }

    public String getLayout() {
        return this.layout;
    }

    public String getHeaderImage() {
        return this.headerImage;
    }

    public String getTeaserImage() {
        return this.teaserImage;
    }

    public String getSpecialImage() {
        return this.specialImage;
    }

    public String getTextOne() {
        return this.textOne;
    }

    public String getTextTwo() {
        return this.textTwo;
    }

    public String getTextDetails() {
        return this.textDetails;
    }

    public String getTextTeaser() {
        return this.textTeaser;
    }

    public TIntArrayList getOfferIds() {
        return this.offerIds;
    }

    public void addOfferId(int offerId) {
        this.offerIds.add(offerId);
    }

    public void addItem(CatalogItem item) {
        this.catalogItems.put(item.getId(), item);
    }

    public TIntObjectMap<CatalogItem> getCatalogItems() {
        return this.catalogItems;
    }

    public CatalogItem getCatalogItem(int id) {
        return this.catalogItems.get(id);
    }

    public ArrayList<Integer> getIncluded() {
        return this.included;
    }

    public THashMap<Integer, CatalogPage> getChildPages() {
        return this.childPages;
    }

    public void addChildPage(CatalogPage page) {
        this.childPages.put(page.getId(), page);

        if (page.getRank() < this.getRank()) {
            page.setRank(this.getRank());
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(CatalogPage page) {
        return this.getOrderNum() - page.getOrderNum();
    }

    @Override
    public abstract void serialize(ServerMessage message);
}
