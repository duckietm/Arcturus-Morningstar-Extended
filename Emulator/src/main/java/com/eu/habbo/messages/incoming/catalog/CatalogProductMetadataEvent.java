package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.CatalogProductMetadataComposer;
import com.eu.habbo.messages.outgoing.catalog.CatalogProductMetadataEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CatalogProductMetadataEvent extends MessageHandler {
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final int GLOBAL_SEARCH_PAGE_ID = -2_147_483_647;
    private static final String SEARCH_PREFIX = "SEARCH:";

    @Override
    public void handle() throws Exception {
        int version = this.packet.readInt();
        String requestId = sanitizeRequestId(this.packet.readString());
        int pageId = this.packet.readInt();
        String catalogMode = this.packet.readString();

        if (this.client == null || this.client.getHabbo() == null) return;

        List<CatalogProductMetadataEntry> entries = List.of();

        if (version == CatalogProductMetadataComposer.PROTOCOL_VERSION) {
            if (pageId > 0) {
                CatalogPage page = Emulator.getGameEnvironment()
                        .getCatalogManager()
                        .getCatalogPage(pageId, CatalogPageType.fromString(catalogMode));

                if (page != null && canOpen(page)) entries = collectEntries(page);
            } else if (pageId == GLOBAL_SEARCH_PAGE_ID && isSearchMode(catalogMode)) {
                SearchRequest search = parseSearchRequest(catalogMode);
                entries = collectSearchEntries(search.catalogType, search.query);
            } else if (pageId < 0) {
                entries = findAccessibleOfferBySprite(-pageId, CatalogPageType.fromString(catalogMode));
            }
        }

        this.client.sendResponse(new CatalogProductMetadataComposer(requestId, pageId, entries));
    }

    private boolean canOpen(CatalogPage page) {
        boolean canSeeCatalogIds = this.client.getHabbo().hasPermission(Permission.ACC_CATALOG_IDS);

        return CatalogPageAccessPolicy.canOpen(
                page.getRank(),
                this.client.getHabbo().getHabboInfo().getRank().getId(),
                page.isEnabled(),
                canSeeCatalogIds);
    }

    /** Normal single-page metadata path. */
    private static List<CatalogProductMetadataEntry> collectEntries(CatalogPage page) {
        List<CatalogProductMetadataEntry> entries = new ArrayList<>();
        List<CatalogItem> offers = new ArrayList<>(page.getCatalogItems().values());
        offers.sort(Comparator.comparingInt(CatalogItem::getId));

        for (CatalogItem offer : offers) {
            List<Item> baseItems = new ArrayList<>(offer.getBaseItems());
            baseItems.sort(Comparator.comparingInt(Item::getId));

            for (Item item : baseItems) {
                entries.add(new CatalogProductMetadataEntry(
                        offer.getId(), item.getId(), item.getSpriteId(), item.allowTrade(), item.allowRecyle()));

                if (entries.size() == CatalogProductMetadataComposer.MAX_ENTRIES) return entries;
            }
        }

        return entries;
    }

    /** Resolve the live purchasable offer used by the room infostand Buy link. */
    private List<CatalogProductMetadataEntry> findAccessibleOfferBySprite(
            int spriteId, CatalogPageType catalogType) {
        if (spriteId <= 0) return List.of();

        CatalogManager manager = Emulator.getGameEnvironment().getCatalogManager();
        List<CatalogPage> pages = new ArrayList<>();

        if (catalogType == CatalogPageType.NORMAL || catalogType == CatalogPageType.BOTH) {
            synchronized (manager.catalogPages) {
                pages.addAll(manager.catalogPages.values());
            }
        }

        if (catalogType == CatalogPageType.BUILDER || catalogType == CatalogPageType.BOTH) {
            synchronized (manager.buildersClubCatalogPages) {
                pages.addAll(manager.buildersClubCatalogPages.values());
            }
        }

        pages.sort(Comparator.comparingInt(CatalogPage::getId));

        for (CatalogPage page : pages) {
            if (page == null || !canOpen(page)) continue;

            List<CatalogItem> offers = new ArrayList<>(page.getCatalogItems().values());
            offers.sort(Comparator.comparingInt(CatalogItem::getId));

            for (CatalogItem offer : offers) {
                int searchOfferId = offer.getSearchOfferId();
                if (searchOfferId <= 0) continue;

                for (Item item : offer.getBaseItems()) {
                    if (item.getSpriteId() != spriteId) continue;

                    return List.of(new CatalogProductMetadataEntry(
                            searchOfferId,
                            item.getId(),
                            spriteId,
                            item.allowTrade(),
                            item.allowRecyle()));
                }
            }
        }

        return List.of();
    }

    /**
     * Global catalog-search metadata path used by CatalogSearchView.
     *
     * The client sends the negative sentinel page id and a mode formatted as:
     *   SEARCH:NORMAL:<query>
     *   SEARCH:BUILDER:<query>
     * Legacy SEARCH:<query> is kept compatible and searches NORMAL.
     *
     * IMPORTANT: search results must expose CatalogItem#getSearchOfferId(), not
     * the catalog row id and not the raw database offer_id.
     */
    private List<CatalogProductMetadataEntry> collectSearchEntries(CatalogPageType catalogType, String query) {
        String needle = normalize(query);
        if (needle.isEmpty()) return List.of();

        CatalogManager manager = Emulator.getGameEnvironment().getCatalogManager();
        List<CatalogPage> pages = new ArrayList<>();

        if (catalogType == CatalogPageType.NORMAL || catalogType == CatalogPageType.BOTH) {
            synchronized (manager.catalogPages) {
                pages.addAll(manager.catalogPages.values());
            }
        }

        if (catalogType == CatalogPageType.BUILDER || catalogType == CatalogPageType.BOTH) {
            synchronized (manager.buildersClubCatalogPages) {
                pages.addAll(manager.buildersClubCatalogPages.values());
            }
        }

        List<CatalogProductMetadataEntry> entries = new ArrayList<>();
        Set<Integer> seenProductClassIds = new HashSet<>();

        for (CatalogPage page : pages) {
            if (page == null || !canOpen(page)) continue;

            List<CatalogItem> offers = new ArrayList<>(page.getCatalogItems().values());
            offers.sort(Comparator.comparingInt(CatalogItem::getId));

            for (CatalogItem offer : offers) {
                int searchOfferId = offer.getSearchOfferId();
                if (searchOfferId <= 0) continue;

                boolean offerMatches = contains(offer.getName(), needle)
                        || contains(offer.getItemId(), needle)
                        || containsInt(offer.getId(), needle)
                        || containsInt(searchOfferId, needle);

                List<Item> baseItems = new ArrayList<>(offer.getBaseItems());
                baseItems.sort(Comparator.comparingInt(Item::getId));

                for (Item item : baseItems) {
                    if (!offerMatches && !itemMatches(item, needle)) continue;

                    int productClassId = item.getSpriteId();
                    if (productClassId <= 0 || !seenProductClassIds.add(productClassId)) continue;

                    entries.add(new CatalogProductMetadataEntry(
                            searchOfferId,
                            item.getId(),
                            productClassId,
                            item.allowTrade(),
                            item.allowRecyle()));

                    if (entries.size() == CatalogProductMetadataComposer.MAX_ENTRIES) return entries;
                }
            }
        }

        return entries;
    }

    private static boolean itemMatches(Item item, String needle) {
        if (contains(item.getName(), needle)
                || containsInt(item.getId(), needle)
                || containsInt(item.getSpriteId(), needle)) {
            return true;
        }

        // Polaris already indexes the configured furnidata display names.
        // This lets global search match the same human-readable names that the
        // Nitro furniture data uses, not only items_base.public_name/classname.
        String displayName = Emulator.getGameEnvironment()
                .getFurnitureTextProvider()
                .getName(item.getName());

        return contains(displayName, needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && normalize(value).contains(needle);
    }

    private static boolean containsInt(int value, String needle) {
        return Integer.toString(value).contains(needle);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isSearchMode(String catalogMode) {
        return catalogMode != null && catalogMode.regionMatches(true, 0, SEARCH_PREFIX, 0, SEARCH_PREFIX.length());
    }

    private static SearchRequest parseSearchRequest(String catalogMode) {
        String payload = catalogMode.substring(SEARCH_PREFIX.length()).trim();
        CatalogPageType catalogType = CatalogPageType.NORMAL;
        String query = payload;

        int separator = payload.indexOf(':');
        if (separator > 0) {
            String typeToken = payload.substring(0, separator).trim();

            if (isCatalogTypeToken(typeToken)) {
                catalogType = CatalogPageType.fromString(typeToken);
                query = payload.substring(separator + 1).trim();
            }
        }

        return new SearchRequest(catalogType, query);
    }

    private static boolean isCatalogTypeToken(String value) {
        if (value == null) return false;

        switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "NORMAL":
            case "BUILDER":
            case "BUILDERS_CLUB":
            case "BC":
            case "BOTH":
                return true;
            default:
                return false;
        }
    }

    private static String sanitizeRequestId(String requestId) {
        if (requestId == null) return "";

        String sanitized = requestId.replaceAll("[\\p{Cntrl}]", "").trim();

        return sanitized.length() <= MAX_REQUEST_ID_LENGTH ? sanitized : sanitized.substring(0, MAX_REQUEST_ID_LENGTH);
    }

    @Override
    public int getRatelimit() {
        return 250;
    }

    @Override
    public String getRatelimitGroup() {
        return "catalog_product_metadata";
    }

    private static final class SearchRequest {
        private final CatalogPageType catalogType;
        private final String query;

        private SearchRequest(CatalogPageType catalogType, String query) {
            this.catalogType = catalogType;
            this.query = query;
        }
    }
}
