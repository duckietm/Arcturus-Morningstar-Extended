package com.eu.habbo.habbohotel.catalog.versioning;

import com.eu.habbo.habbohotel.catalog.CatalogPageType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns validation issues into the restricted SQL the Catalog Studio import understands, using the full live
 * snapshot and the validator's reference data so every repair is deterministic:
 *
 * <ul>
 *   <li>missing parent / cycle → page moved to the root</li>
 *   <li>unsupported layout → known alias, else default_3x3</li>
 *   <li>duplicate sibling order → the whole sibling group is renumbered in its current order</li>
 *   <li>bad includes → only the invalid tokens are dropped</li>
 *   <li>visible page under a hidden ancestor → hidden</li>
 *   <li>offer on a missing page / sound offer without song → removed (undoable from the history)</li>
 *   <li>missing / invalid item tokens → dropped; an offer left without items is removed</li>
 *   <li>negative prices → clamped; unsupported points currency → folded into credits</li>
 *   <li>limited stack below sales → raised to the sold count</li>
 * </ul>
 */
public final class CatalogAutoFixPlanner {

    public record Plan(List<String> statements, int issuesAddressed, Map<String, Integer> byCode) {
        public String describe() {
            if (byCode.isEmpty()) return "nessuna correzione";
            StringBuilder sb = new StringBuilder();
            byCode.forEach((code, count) -> {
                if (sb.length() > 0) sb.append(", ");
                sb.append(code).append(" ×").append(count);
            });
            return sb.toString();
        }
    }

    /** One entity's pending changes: either a delete or a set of column assignments. */
    private static final class EntityFix {
        final String table;
        final CatalogPageType catalogType;
        final int id;
        boolean delete;
        final Map<String, String> sets = new LinkedHashMap<>();

        EntityFix(String table, CatalogPageType catalogType, int id) {
            this.table = table;
            this.catalogType = catalogType;
            this.id = id;
        }

        String toSql() {
            String selector = " WHERE id = " + id + (catalogType == CatalogPageType.BUILDER ? " AND catalog_type = 'BUILDER'" : "") + ";";
            if (delete) return "DELETE FROM " + table + selector;
            if (sets.isEmpty()) return null;
            StringBuilder sb = new StringBuilder("UPDATE ").append(table).append(" SET ");
            boolean first = true;
            for (Map.Entry<String, String> set : sets.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(set.getKey()).append(" = ").append(set.getValue());
            }
            return sb.append(selector).toString();
        }
    }

    private CatalogAutoFixPlanner() {}

    public static Plan plan(
            CatalogVersionSnapshot snapshot, List<CatalogValidationIssue> issues, CatalogValidationReferenceData reference) {
        Map<String, EntityFix> fixes = new LinkedHashMap<>();
        Map<String, Integer> byCode = new LinkedHashMap<>();
        Set<String> renumberedGroups = new HashSet<>();
        int addressed = 0;

        for (CatalogValidationIssue issue : issues) {
            boolean handled = false;

            if (issue.entityType() == CatalogEntityType.PAGE) {
                for (CatalogPageSnapshot page : pages(snapshot, issue.entityId())) {
                    handled |= fixPage(snapshot, reference, fixes, renumberedGroups, issue.code(), page);
                }
            } else {
                for (CatalogOfferSnapshot offer : offers(snapshot, issue.entityId())) {
                    handled |= fixOffer(snapshot, reference, fixes, issue.code(), offer);
                }
            }

            if (handled) {
                addressed++;
                byCode.merge(issue.code(), 1, Integer::sum);
            }
        }

        List<String> statements = new ArrayList<>();
        for (EntityFix fix : fixes.values()) {
            String sql = fix.toSql();
            if (sql != null) statements.add(sql);
        }

        return new Plan(statements, addressed, byCode);
    }

    private static List<CatalogPageSnapshot> pages(CatalogVersionSnapshot snapshot, int pageId) {
        List<CatalogPageSnapshot> result = new ArrayList<>(2);
        snapshot.page(CatalogPageType.NORMAL, pageId).ifPresent(result::add);
        snapshot.page(CatalogPageType.BUILDER, pageId).ifPresent(result::add);
        return result;
    }

    private static List<CatalogOfferSnapshot> offers(CatalogVersionSnapshot snapshot, int offerId) {
        List<CatalogOfferSnapshot> result = new ArrayList<>(2);
        snapshot.offer(CatalogPageType.NORMAL, offerId).ifPresent(result::add);
        snapshot.offer(CatalogPageType.BUILDER, offerId).ifPresent(result::add);
        return result;
    }

    private static EntityFix fixFor(Map<String, EntityFix> fixes, String table, CatalogPageType type, int id) {
        return fixes.computeIfAbsent(table + ":" + type + ":" + id, ignored -> new EntityFix(table, type, id));
    }

    // ---- pages ---------------------------------------------------------------------------------------------------

    private static boolean fixPage(
            CatalogVersionSnapshot snapshot,
            CatalogValidationReferenceData reference,
            Map<String, EntityFix> fixes,
            Set<String> renumberedGroups,
            String code,
            CatalogPageSnapshot page) {
        CatalogPageType type = page.catalogType();

        switch (code) {
            case "PAGE_PARENT_MISSING": {
                if (page.parentId() <= 0 || snapshot.page(type, page.parentId()).isPresent()) return false;
                fixFor(fixes, "catalog_pages", type, page.pageId()).sets.put("parent_id", "-1");
                return true;
            }
            case "PAGE_CYCLE": {
                if (page.parentId() <= 0) return false;
                fixFor(fixes, "catalog_pages", type, page.pageId()).sets.put("parent_id", "-1");
                return true;
            }
            case "PAGE_LAYOUT_UNSUPPORTED": {
                if (reference.supportedLayouts().contains(page.pageLayout())) return false;
                fixFor(fixes, "catalog_pages", type, page.pageId())
                        .sets.put("page_layout", string(resolveLayout(page.pageLayout(), reference.supportedLayouts())));
                return true;
            }
            case "PAGE_SIBLING_ORDER_DUPLICATE": {
                String group = type + ":" + page.parentId();
                if (!renumberedGroups.add(group)) return true; // the whole group was already renumbered

                List<CatalogPageSnapshot> siblings = new ArrayList<>();
                for (CatalogPageSnapshot candidate : snapshot.pages()) {
                    if (candidate.catalogType() == type && candidate.parentId() == page.parentId()) siblings.add(candidate);
                }
                siblings.sort(Comparator.comparingInt(CatalogPageSnapshot::orderNum).thenComparingInt(CatalogPageSnapshot::pageId));

                boolean changed = false;
                for (int index = 0; index < siblings.size(); index++) {
                    CatalogPageSnapshot sibling = siblings.get(index);
                    if (sibling.orderNum() == index) continue;
                    fixFor(fixes, "catalog_pages", type, sibling.pageId()).sets.put("order_num", Integer.toString(index));
                    changed = true;
                }
                return changed;
            }
            case "PAGE_INCLUDE_MISSING":
            case "PAGE_INCLUDE_INVALID": {
                String includes = page.includes() == null ? "" : page.includes();
                if (includes.isBlank()) return false;
                Set<Integer> kept = new LinkedHashSet<>();
                for (String raw : includes.split(";", -1)) {
                    try {
                        int includedId = Integer.parseInt(raw.trim());
                        if (includedId > 0 && snapshot.page(type, includedId).isPresent()) kept.add(includedId);
                    } catch (NumberFormatException ignored) {
                        // invalid token: dropped
                    }
                }
                StringBuilder rebuilt = new StringBuilder();
                for (int includedId : kept) {
                    if (rebuilt.length() > 0) rebuilt.append(';');
                    rebuilt.append(includedId);
                }
                if (rebuilt.toString().equals(includes)) return false;
                fixFor(fixes, "catalog_pages", type, page.pageId()).sets.put("includes", string(rebuilt.toString()));
                return true;
            }
            case "PAGE_ANCESTOR_NOT_AVAILABLE": {
                if (!page.visible()) return false;
                fixFor(fixes, "catalog_pages", type, page.pageId()).sets.put("visible", "0");
                return true;
            }
            default:
                return false;
        }
    }

    private static String resolveLayout(String layout, Set<String> supported) {
        String normalized = layout == null ? "" : layout.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (supported.contains(normalized)) return normalized;

        String alias = switch (normalized) {
            case "default3x3", "default", "default_3x3_layout" -> "default_3x3";
            case "color_grouping", "colorgrouping", "default_3x3_colour_grouping" -> "default_3x3_color_grouping";
            case "spaces_new" -> "spaces";
            case "frontpage4", "front_page" -> "frontpage";
            case "pets_customization", "pet_customization" -> "petcustomization";
            case "guild_frontpage", "guild" -> "guilds";
            case "vip", "club_vip" -> "vip_buy";
            case "single_bundle_layout", "bundle" -> "single_bundle";
            case "room_bundles", "roombundle" -> "room_bundle";
            default -> "";
        };

        if (!alias.isEmpty() && supported.contains(alias)) return alias;
        return supported.contains("default_3x3") ? "default_3x3" : supported.iterator().next();
    }

    // ---- offers --------------------------------------------------------------------------------------------------

    private static boolean fixOffer(
            CatalogVersionSnapshot snapshot,
            CatalogValidationReferenceData reference,
            Map<String, EntityFix> fixes,
            String code,
            CatalogOfferSnapshot offer) {
        CatalogPageType type = offer.catalogType();
        EntityFix fix = fixFor(fixes, "catalog_items", type, offer.offerId());
        if (fix.delete) return true; // already scheduled for removal

        switch (code) {
            case "OFFER_PAGE_MISSING": {
                if (snapshot.page(type, offer.pageId()).isPresent()) return false;
                fix.delete = true;
                return true;
            }
            case "OFFER_SOUND_REFERENCE_MISSING": {
                fix.delete = true;
                return true;
            }
            case "OFFER_ITEM_INVALID":
            case "OFFER_ITEM_MISSING": {
                String itemIds = offer.itemIds() == null ? "" : offer.itemIds();
                List<String> kept = new ArrayList<>();
                for (String token : itemIds.split(";", -1)) {
                    String[] parts = token.trim().split(":", -1);
                    try {
                        if (parts.length > 2 || parts[0].isBlank()) continue;
                        int itemId = Integer.parseInt(parts[0].trim());
                        int quantity = parts.length == 2 ? Integer.parseInt(parts[1].trim()) : 1;
                        if (itemId <= 0 || quantity <= 0 || !reference.itemDefinitionIds().contains(itemId)) continue;
                        kept.add(quantity > 1 ? itemId + ":" + quantity : Integer.toString(itemId));
                    } catch (NumberFormatException ignored) {
                        // invalid token: dropped
                    }
                }
                if (kept.isEmpty()) {
                    fix.delete = true;
                    return true;
                }
                String rebuilt = String.join(";", kept);
                if (rebuilt.equals(itemIds)) return false;
                fix.sets.put("item_ids", string(rebuilt));
                return true;
            }
            case "OFFER_PRICE_INVALID": {
                boolean changed = false;
                if (offer.costCredits() < 0) {
                    fix.sets.put("cost_credits", "0");
                    changed = true;
                }
                if (offer.costPoints() < 0) {
                    fix.sets.put("cost_points", "0");
                    changed = true;
                }
                if (offer.amount() <= 0) {
                    fix.sets.put("amount", "1");
                    changed = true;
                }
                return changed;
            }
            case "OFFER_CURRENCY_UNSUPPORTED": {
                if (offer.costPoints() <= 0 || reference.currencyTypes().contains(offer.pointsType())) return false;
                // Nobody can hold that currency: fold the points into credits so the offer stays purchasable.
                fix.sets.put("cost_credits", Integer.toString(Math.max(0, offer.costCredits()) + Math.max(0, offer.costPoints())));
                fix.sets.put("cost_points", "0");
                fix.sets.put("points_type", "0");
                return true;
            }
            case "OFFER_LIMITED_STACK_BELOW_SALES": {
                int sold = reference.liveLimitedSells().getOrDefault(offer.offerId(), 0);
                if (offer.limitedStack() == 0 || offer.limitedStack() >= sold) return false;
                fix.sets.put("limited_stack", Integer.toString(sold));
                return true;
            }
            default:
                return false;
        }
    }

    private static String string(String value) {
        return "'" + (value == null ? "" : value.replace("'", "''")) + "'";
    }
}
