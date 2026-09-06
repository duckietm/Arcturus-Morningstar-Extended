import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const emulatorRoot = path.resolve(import.meta.dirname, '..');
const xabboRoot = path.join(process.env.LOCALAPPDATA ?? path.join(os.homedir(), 'AppData', 'Local'), 'xabbo', 'bss');
const furnidataPath = path.join(xabboRoot, 'furnidatanew.json');
const cataloguePath = path.join(xabboRoot, 'catalogue.json');
const statePath = path.join(xabboRoot, 'cache', 'state');
const migrationPath = path.join(emulatorRoot, 'src', 'main', 'resources', 'db', 'migration',
    'V20260810160000__xabbo_bss_furni_viewer.sql');

const mysqlCandidates = [
    'C:\\Program Files\\MariaDB 12.3\\bin\\mysql.exe',
    'C:\\Program Files\\MariaDB 12.2\\bin\\mysql.exe',
    'mysql.exe',
];
const mysql = mysqlCandidates.find(candidate => candidate === 'mysql.exe' || fs.existsSync(candidate));
if (!mysql) throw new Error('mysql.exe non trovato.');
if (!fs.existsSync(furnidataPath)) throw new Error(`Furnidata Xabbo non trovato: ${furnidataPath}`);
if (!fs.existsSync(cataloguePath)) throw new Error(`Snapshot catalogo Xabbo non trovato: ${cataloguePath}`);

const furnidata = JSON.parse(fs.readFileSync(furnidataPath, 'utf8'));
const savedCatalogue = JSON.parse(fs.readFileSync(cataloguePath, 'utf8'));
const stateFiles = fs.existsSync(statePath)
    ? fs.readdirSync(statePath, { recursive: true, withFileTypes: true })
        .filter(entry => entry.isFile())
        .map(entry => path.join(entry.parentPath, entry.name))
    : [];
const completeMaps = [];
for (const file of stateFiles) {
    try {
        const value = JSON.parse(fs.readFileSync(file, 'utf8'));
        if (value?.Complete === true && Array.isArray(value.Offers) && value.Offers.length > 0)
            completeMaps.push({ file, value, savedAt: Date.parse(value.SavedAt ?? 0) || fs.statSync(file).mtimeMs });
    } catch { /* unrelated cache entry */ }
}
completeMaps.sort((a, b) => b.savedAt - a.savedAt);
const currentMap = completeMaps[0] ?? null;
const soldEntries = currentMap?.value?.Offers ?? savedCatalogue.TypeToOffer ?? [];
const catalogueSource = currentMap?.file ?? cataloguePath;
const floor = furnidata?.roomitemtypes?.furnitype ?? [];
const wall = furnidata?.wallitemtypes?.furnitype ?? [];

const htmlDecode = value => String(value ?? '')
    .replace(/&#x([0-9a-f]+);/gi, (_, hex) => String.fromCodePoint(Number.parseInt(hex, 16)))
    .replace(/&#(\d+);/g, (_, decimal) => String.fromCodePoint(Number.parseInt(decimal, 10)))
    .replaceAll('&nbsp;', ' ').replaceAll('&amp;', '&').replaceAll('&quot;', '"')
    .replaceAll('&#39;', "'").replaceAll('&lt;', '<').replaceAll('&gt;', '>');

const normalize = value => htmlDecode(value).toLocaleLowerCase('it-IT').replace(/\s+/g, ' ').trim();
const explicitCredits = description => /\bcredit(?:o|i)\b/.test(normalize(description));
const numberWords = new Map(Object.entries({ uno: 1, una: 1, due: 2, tre: 3, quattro: 4, cinque: 5,
    sei: 6, sette: 7, otto: 8, nove: 9, dieci: 10 }));
const specialRareSets = new Set(['opti_dragosma_ragd', 'qt_smar_agddragon', 'oni_dragon_geant_poker']);

function parseRare(description) {
    const desc = normalize(description);
    if (!desc || /non è raro|non e' raro|valore [rgb] al filtro|misterioso|colpi per/.test(desc)) return null;

    let match = desc.match(/vale\s+(\d+)\s*(?:crediti|credito)?\s+(?:oppure|o)\s+(\d+)\s*(diamanti|diamante|crediti|credito|cristalli)/);
    if (match) {
        const first = Number(match[1]);
        const second = Number(match[2]);
        if (match[3].startsWith('diamant')) return { credits: first, diamonds: second, crystals: 0 };
        if (match[3].startsWith('cristall')) return { credits: first, diamonds: 0, crystals: second };
        return { credits: Math.max(first, second), diamonds: 0, crystals: 0 };
    }

    match = desc.match(/vale\s+([a-zà-ù]+)\s+(?:piccoli\s+|piccole\s+|medi\s+|medie\s+|grandi\s+)?cristall/);
    if (match && numberWords.has(match[1])) return { credits: 0, diamonds: 0, crystals: numberWords.get(match[1]) };

    match = desc.match(/(?:^|questo raro |questo furni |la coppia |il raro )vale\s+(\d+)\s*(diamanti|diamante|crediti|credito|cristalli)?/)
        ?? desc.match(/premio del valore di\s+(\d+)\s*(diamanti|diamante|crediti|credito)?/);
    if (!match) return null;
    const value = Number(match[1]);
    if (!value) return null;
    const currency = match[2] ?? '';
    if (currency.startsWith('diamant')) return { credits: 0, diamonds: value, crystals: 0 };
    if (currency.startsWith('cristall')) return { credits: 0, diamonds: 0, crystals: value };
    return { credits: value, diamonds: 0, crystals: 0 };
}

function typed(items, isWall) {
    const seen = new Set();
    const result = [];
    for (const item of items) {
        const id = Number(item.id);
        if (!Number.isInteger(id) || id < -2147483648 || id > 2147483647 || seen.has(id)) continue;
        seen.add(id);
        result.push({
            ...item,
            id,
            classname: String(item.classname ?? ''),
            name: htmlDecode(item.name || item.classname || `#${item.id}`),
            description: htmlDecode(item.description ?? ''),
            isWall,
            dbType: isWall ? 'i' : 's',
        });
    }
    return result;
}

const all = [...typed(floor, false), ...typed(wall, true)];
const sold = new Set(soldEntries.map(entry => `${entry.IsWall ? 'i' : 's'}:${Number(entry.Type)}`));
const byTypedId = new Map(all.map(item => [`${item.dbType}:${item.id}`, item]));
const soldNames = new Set([...sold].map(key => byTypedId.get(key)).filter(Boolean)
    .map(item => `${item.dbType}:${item.classname}`));

function rareValue(item) {
    const parsed = parseRare(item.description);
    if (parsed) {
        if (parsed.credits > 0 && parsed.diamonds === 0 && parsed.crystals === 0 && explicitCredits(item.description)) return null;
        return parsed;
    }
    return specialRareSets.has(item.classname) ? { credits: 0, diamonds: 0, crystals: 0 } : null;
}

const rareByKey = new Map();
for (const item of all) {
    const value = rareValue(item);
    if (!value) continue;
    const typedId = `${item.dbType}:${item.id}`;
    const typedName = `${item.dbType}:${item.classname}`;
    if (sold.has(typedId) || soldNames.has(typedName)) continue;
    rareByKey.set(typedId, { ...item, ...value });
    if (item.classname) soldNames.add(typedName);
}

const rareFloor = [...rareByKey.values()].filter(item => !item.isWall);
const rareWall = [...rareByKey.values()].filter(item => item.isWall);
const limited = [];
for (const entry of currentMap?.value?.LimitedTypes ?? []) {
    const key = `${entry.IsWall ? 'i' : 's'}:${Number(entry.Type)}`;
    if (sold.has(key)) continue;
    const item = byTypedId.get(key);
    if (!item) continue;
    limited.push(item);
    if (item.classname) soldNames.add(`${item.dbType}:${item.classname}`);
}
const limitedFloor = limited.filter(item => !item.isWall).sort((a, b) => b.id - a.id);
const limitedWall = limited.filter(item => item.isWall).sort((a, b) => b.id - a.id);
const unavailableFloor = all.filter(item => !item.isWall && !sold.has(`s:${item.id}`)
    && !soldNames.has(`s:${item.classname}`) && !rareByKey.has(`s:${item.id}`)).sort((a, b) => b.id - a.id);
const unavailableWall = all.filter(item => item.isWall && !sold.has(`i:${item.id}`)
    && !soldNames.has(`i:${item.classname}`) && !rareByKey.has(`i:${item.id}`)).sort((a, b) => b.id - a.id);

const itemRows = execFileSync(mysql, ['-uroot', '-N', '-B', 'polaris_local', '-e',
    "SELECT id,sprite_id,item_name,public_name,type FROM items_base WHERE type IN ('s','i')"],
    { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).trim().split(/\r?\n/).filter(Boolean)
    .map(line => { const [id, sprite, itemName, publicName, type] = line.split('\t');
        return { id: Number(id), sprite: Number(sprite), itemName, publicName, type }; });
const bases = new Map();
for (const row of itemRows) {
    const key = `${row.type}:${row.sprite}`;
    const list = bases.get(key) ?? [];
    list.push(row);
    bases.set(key, list);
}

const pageRowsFromDb = execFileSync(mysql, ['-uroot', '-N', '-B', 'polaris_local', '-e',
    'SELECT id,parent_id,caption,min_rank FROM catalog_pages'],
    { encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 }).trim().split(/\r?\n/).filter(Boolean)
    .map(line => { const [id, parent, caption, rank] = line.split('\t');
        return { id: Number(id), parent: Number(parent), caption, rank: Number(rank) }; });
const localPages = new Map(pageRowsFromDb.map(page => [page.id, page]));
const publicSourcePages = new Set(soldEntries.map(entry => Number(entry.Page)).filter(id => id > 0));
const publicLocalPages = new Set();
const publicRootsUnderStaff = new Set();
for (const sourceId of publicSourcePages) {
    let page = localPages.get(sourceId);
    const visited = new Set();
    while (page && page.id > 0 && page.id !== 7 && !visited.has(page.id)) {
        visited.add(page.id);
        publicLocalPages.add(page.id);
        if (page.parent === 7) publicRootsUnderStaff.add(page.id);
        page = localPages.get(page.parent);
    }
}
const publicWiredPages = [...publicLocalPages]
    .filter(id => id !== 1800)
    .filter(id => /wired/i.test(localPages.get(id)?.caption ?? ''));

let nextBaseId = 2136000000;
const generatedBases = [];

function attachBase(items) {
    const attached = [];
    const missing = [];
    for (const item of items) {
        const candidates = bases.get(`${item.dbType}:${item.id}`) ?? [];
        const base = candidates.find(row => row.itemName === item.classname) ?? candidates[0];
        if (!base) {
            missing.push(item);
            const generated = { ...item, baseId: nextBaseId++ };
            generatedBases.push(generated);
            attached.push(generated);
            continue;
        }
        attached.push({ ...item, baseId: base.id });
    }
    return { attached, missing };
}

const classified = {
    rareFloor: attachBase(rareFloor), rareWall: attachBase(rareWall),
    limitedFloor: attachBase(limitedFloor), limitedWall: attachBase(limitedWall),
    unavailableFloor: attachBase(unavailableFloor), unavailableWall: attachBase(unavailableWall),
};
const esc = value => `'${String(value ?? '').replaceAll('\\', '\\\\').replaceAll("'", "''")}'`;
const viewerRoot = 2139999999;
const categories = {
    rareFloor: { id: 2145999940, save: 'bss_viewer_rare_floor', caption: 'Rari da pavimento', pageBase: 2139000000 },
    rareWall: { id: 2145999939, save: 'bss_viewer_rare_wall', caption: 'Rari da parete', pageBase: 2139100000 },
    unavailableFloor: { id: 2145999938, save: 'bss_viewer_unav_floor', caption: 'Non disponibili - pavimento', pageBase: 2139200000 },
    unavailableWall: { id: 2145999937, save: 'bss_viewer_unav_wall', caption: 'Non disponibili - parete', pageBase: 2139300000 },
};

function groupRares(items) {
    const map = new Map();
    for (const item of items) {
        const key = `${item.credits}:${item.diamonds}:${item.crystals}`;
        const group = map.get(key) ?? [];
        group.push(item);
        map.set(key, group);
    }
    return [...map.values()].sort((a, b) => {
        const av = a[0].credits + a[0].diamonds + a[0].crystals;
        const bv = b[0].credits + b[0].diamonds + b[0].crystals;
        return bv - av;
    });
}

function rareLabel(item, count) {
    let value;
    if (item.credits > 0 && item.diamonds > 0) value = `${item.credits} crediti + ${item.diamonds} diamanti`;
    else if (item.diamonds > 0) value = `${item.diamonds} diamanti`;
    else if (item.crystals > 0) value = `${item.crystals} cristalli`;
    else if (item.credits > 0) value = `${item.credits} BSS points`;
    else value = 'Valore sconosciuto';
    return `${value} (${count})`;
}

const pages = [];
for (const key of ['rareFloor', 'rareWall']) {
    const category = categories[key];
    groupRares(classified[key].attached).forEach((items, index) => pages.push({
        id: category.pageBase + index, parent: category.id, order: index + 1,
        save: `${category.save}_${index + 1}`, caption: rareLabel(items[0], items.length), items, rare: true,
    }));
}
for (const key of ['unavailableFloor', 'unavailableWall']) {
    const category = categories[key];
    const items = classified[key].attached;
    const count = Math.ceil(items.length / 500);
    for (let index = 0; index < count; index++) {
        const slice = items.slice(index * 500, (index + 1) * 500);
        pages.push({ id: category.pageBase + index, parent: category.id, order: index + 1,
            save: `${category.save}_${index + 1}`, caption: `${index + 1}/${count} (${slice.length})`, items: slice, rare: false });
    }
}
if (classified.limitedFloor.attached.length > 0) pages.push({
    id: 2139400000, parent: viewerRoot, order: 5, save: 'bss_viewer_ltd_floor',
    caption: `Edizioni limitate - pavimento (${classified.limitedFloor.attached.length})`,
    items: classified.limitedFloor.attached, rare: false,
});
if (classified.limitedWall.attached.length > 0) pages.push({
    id: 2139400001, parent: viewerRoot, order: 6, save: 'bss_viewer_ltd_wall',
    caption: `Edizioni limitate - parete (${classified.limitedWall.attached.length})`,
    items: classified.limitedWall.attached, rare: false,
});

const pageRows = pages.map(page => `(${page.id},${page.parent},${esc(page.save.slice(0, 25))},${esc(page.caption.slice(0, 128))},'default_3x3',1,3011,7,${page.order},'1','1','0','NORMAL','0','','','',NULL,NULL,NULL,NULL,0,'')`);
const baseRows = generatedBases.map(item => {
    const width = Math.max(1, Number(item.xdim) || 1);
    const length = Math.max(1, Number(item.ydim) || 1);
    return `(${item.baseId},${item.id},${esc(item.name.slice(0, 56))},${esc(item.classname.slice(0, 70))},${esc(item.dbType)},${width},${length},1.00,${item.canstandon ? 1 : 0},${item.cansiton ? 1 : 0},${item.canlayon ? 1 : 0},${item.canstandon ? 1 : 0},'default',1,${esc(String(item.customparams ?? '').slice(0, 256))})`;
});
let nextOfferId = 2137000000;
const offerRows = [];
for (const page of pages) {
    page.items.forEach((item, index) => {
        const id = nextOfferId++;
        const credits = page.rare && item.diamonds > 0 ? item.credits : 0;
        const points = page.rare ? (item.diamonds > 0 ? item.diamonds : item.credits || item.crystals) : 0;
        const pointsType = page.rare ? (item.diamonds > 0 ? 5 : 103) : 0;
        offerRows.push(`(${id},${esc(item.baseId)},${page.id},${esc(item.classname.slice(0, 100))},${credits},${points},${pointsType},1,0,0,${index + 1},${id},0,'','1','0')`);
    });
}

function batchedInsert(table, columns, rows, batchSize = 250) {
    const statements = [];
    for (let i = 0; i < rows.length; i += batchSize)
        statements.push(`INSERT INTO \`${table}\` (${columns}) VALUES\n${rows.slice(i, i + batchSize).join(',\n')};`);
    return statements.join('\n\n');
}

const total = Object.values(classified).reduce((sum, value) => sum + value.attached.length, 0);
const categoryRows = Object.entries(categories).map(([key, category], index) => {
    const count = classified[key].attached.length;
    return `(${category.id},${viewerRoot},${esc(category.save)},${esc(`${category.caption} (${count})`)},'default_3x3',1,3011,7,${index + 1},'1','1','0','NORMAL','0','','','',NULL,NULL,NULL,NULL,0,'')`;
});

const sql = `-- Generated from the same Xabbo BssGameData/BssCatalogInjector sources and rules.\n` +
`-- furnidata: ${furnidataPath.replaceAll('\\', '/')}\n-- complete typed catalogue map: ${catalogueSource.replaceAll('\\', '/')}\n` +
`-- rare floor=${classified.rareFloor.attached.length}, rare wall=${classified.rareWall.attached.length}, ` +
`unavailable floor=${classified.unavailableFloor.attached.length}, unavailable wall=${classified.unavailableWall.attached.length}\n\n` +
`-- Anything present in BSS's complete public catalogue map must not remain staff-only locally.\n` +
`UPDATE \`catalog_pages\` SET \`min_rank\`=1 WHERE \`id\` IN (${[...publicLocalPages].join(',') || '0'});\n` +
`UPDATE \`catalog_pages\` SET \`parent_id\`=1701 WHERE \`id\` IN (${[...publicRootsUnderStaff].join(',') || '0'}) AND \`parent_id\`=7;\n` +
`UPDATE \`catalog_pages\` SET \`parent_id\`=1800, \`min_rank\`=1 WHERE \`id\` IN (${publicWiredPages.join(',') || '0'});\n\n` +
`DELETE ci FROM \`catalog_items\` ci JOIN \`catalog_pages\` p ON p.id=ci.page_id\n` +
`WHERE p.id IN (2145999940,2145999939,2145999938,2145999937,${viewerRoot})\n` +
`   OR p.parent_id IN (2145999940,2145999939,2145999938,2145999937,${viewerRoot})\n` +
`   OR p.id BETWEEN 2139000000 AND 2139499999;\n` +
`DELETE FROM \`catalog_pages\` WHERE parent_id IN (2145999940,2145999939,2145999938,2145999937,${viewerRoot}) OR id BETWEEN 2139000000 AND 2139499999;\n\n` +
`INSERT INTO \`catalog_pages\` (id,parent_id,caption_save,caption,page_layout,icon_color,icon_image,min_rank,order_num,visible,enabled,club_only,catalog_mode,vip_only,page_headline,page_teaser,page_special,page_text1,page_text2,page_text_details,page_text_teaser,room_id,includes) VALUES\n` +
`(${viewerRoot},7,'bss_viewer','Visualizzatore Furni BSS (${total})','default_3x3',1,3011,7,900,'1','1','0','NORMAL','0','','','',NULL,NULL,NULL,NULL,0,'')\n` +
`ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),caption_save=VALUES(caption_save),caption=VALUES(caption),page_layout=VALUES(page_layout),icon_image=VALUES(icon_image),min_rank=VALUES(min_rank),order_num=VALUES(order_num),visible=VALUES(visible),enabled=VALUES(enabled);\n\n` +
`INSERT INTO \`catalog_pages\` (id,parent_id,caption_save,caption,page_layout,icon_color,icon_image,min_rank,order_num,visible,enabled,club_only,catalog_mode,vip_only,page_headline,page_teaser,page_special,page_text1,page_text2,page_text_details,page_text_teaser,room_id,includes) VALUES\n${categoryRows.join(',\n')}\n` +
`ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),caption_save=VALUES(caption_save),caption=VALUES(caption),page_layout=VALUES(page_layout),icon_image=VALUES(icon_image),min_rank=VALUES(min_rank),order_num=VALUES(order_num),visible=VALUES(visible),enabled=VALUES(enabled);\n\n` +
batchedInsert('catalog_pages', '`id`,`parent_id`,`caption_save`,`caption`,`page_layout`,`icon_color`,`icon_image`,`min_rank`,`order_num`,`visible`,`enabled`,`club_only`,`catalog_mode`,`vip_only`,`page_headline`,`page_teaser`,`page_special`,`page_text1`,`page_text2`,`page_text_details`,`page_text_teaser`,`room_id`,`includes`', pageRows) + '\n\n' +
batchedInsert('items_base', '`id`,`sprite_id`,`public_name`,`item_name`,`type`,`width`,`length`,`stack_height`,`allow_stack`,`allow_sit`,`allow_lay`,`allow_walk`,`interaction_type`,`interaction_modes_count`,`customparams`', baseRows) + '\n\n' +
batchedInsert('catalog_items', '`id`,`item_ids`,`page_id`,`catalog_name`,`cost_credits`,`cost_points`,`points_type`,`amount`,`limited_stack`,`limited_sells`,`order_number`,`offer_id`,`song_id`,`extradata`,`have_offer`,`club_only`', offerRows) + '\n';

fs.writeFileSync(migrationPath, sql, 'utf8');
const summary = Object.fromEntries(Object.entries(classified).map(([key, value]) => [key,
    { imported: value.attached.length, generatedBase: value.missing.length }]));
console.log(JSON.stringify({ migrationPath, pages: pages.length, offers: offerRows.length,
    generatedBaseItems: generatedBases.length, publicPagesPromoted: publicLocalPages.size,
    publicRootsMovedFromStaff: publicRootsUnderStaff.size, summary }, null, 2));
