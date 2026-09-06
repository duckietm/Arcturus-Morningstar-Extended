package com.eu.habbo.habbohotel.catalog;

import com.eu.habbo.habbohotel.items.Item;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable trigram index for rank-aware catalog searches. */
public final class CatalogSearchIndex {
    public record Hit(CatalogItem offer, Item item) {}

    private final Map<String, List<Entry>> postings;

    private CatalogSearchIndex(Map<String, List<Entry>> postings) {
        this.postings = postings;
    }

    public static CatalogSearchIndex empty() {
        return new CatalogSearchIndex(Map.of());
    }

    public static CatalogSearchIndex build(Iterable<CatalogPage> pages) {
        Map<String, List<Entry>> mutable = new HashMap<>();
        List<CatalogPage> sortedPages = new ArrayList<>();
        pages.forEach(sortedPages::add);
        sortedPages.sort(Comparator.comparingInt(CatalogPage::getId));

        for (CatalogPage page : sortedPages) {
            List<CatalogItem> offers = new ArrayList<>(page.getCatalogItems().values());
            offers.sort(Comparator.comparingInt(CatalogItem::getId));

            for (CatalogItem offer : offers) {
                if (!offer.isHaveOffer()) continue;
                List<Item> items = new ArrayList<>(offer.getBaseItems());
                items.sort(Comparator.comparingInt(Item::getId));

                for (Item item : items) {
                    String text = normalize(String.join(
                            " ",
                            safe(offer.getName()),
                            safe(item.getName()),
                            safe(item.getFullName()),
                            safe(item.getDisplayName())));
                    if (text.length() < 3) continue;

                    Entry entry = new Entry(page, offer, item, text);
                    for (String trigram : trigrams(text)) {
                        mutable.computeIfAbsent(trigram, ignored -> new ArrayList<>()).add(entry);
                    }
                }
            }
        }

        Map<String, List<Entry>> immutable = new HashMap<>(mutable.size());
        mutable.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return new CatalogSearchIndex(Map.copyOf(immutable));
    }

    public List<Hit> search(String query, int userRank, int maximumResults) {
        String needle = normalize(query);
        if (needle.length() < 3 || maximumResults <= 0) return List.of();

        List<Entry> candidates = null;
        for (String trigram : trigrams(needle)) {
            List<Entry> posting = this.postings.get(trigram);
            if (posting == null) return List.of();
            if (candidates == null || posting.size() < candidates.size()) candidates = posting;
        }
        if (candidates == null) return List.of();

        List<Hit> results = new ArrayList<>();
        Set<Integer> seenOffers = new HashSet<>();
        for (Entry entry : candidates) {
            CatalogPage page = entry.page();
            if (!page.isVisible() || !page.isEnabled() || page.getRank() > userRank) continue;
            if (!entry.text().contains(needle) || !seenOffers.add(entry.offer().getId())) continue;
            results.add(new Hit(entry.offer(), entry.item()));
            if (results.size() >= maximumResults) break;
        }
        return List.copyOf(results);
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Set<String> trigrams(String value) {
        Set<String> result = new HashSet<>();
        for (int index = 0; index <= value.length() - 3; index++) {
            result.add(value.substring(index, index + 3));
        }
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Entry(CatalogPage page, CatalogItem offer, Item item, String text) {}
}
