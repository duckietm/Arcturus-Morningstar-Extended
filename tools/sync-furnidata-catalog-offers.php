<?php
declare(strict_types=1);

// Synchronise Nitro's FurnitureData offerid fields with the live catalog.
// An item without an active offer gets -1, so the infostand never opens a
// dead catalog link.

$root = dirname(__DIR__);
$config = $root . DIRECTORY_SEPARATOR . 'Emulator' . DIRECTORY_SEPARATOR . 'config.ini';
$furniData = dirname($root) . DIRECTORY_SEPARATOR . 'AtomCMS' . DIRECTORY_SEPARATOR . 'public' . DIRECTORY_SEPARATOR . 'nitro-assets' . DIRECTORY_SEPARATOR . 'FurnitureData.json';

if (!is_file($config) || !is_file($furniData)) {
    throw new RuntimeException('Polaris config.ini or FurnitureData.json was not found.');
}

$settings = parse_ini_file($config, false, INI_SCANNER_RAW);
$host = getenv('POLARIS_DB_HOST') ?: ($settings['db.hostname'] ?? '127.0.0.1');
$database = $settings['db.database'] ?? '';
$username = $settings['db.username'] ?? '';
$password = $settings['db.password'] ?? '';
$port = (int) ($settings['db.port'] ?? 3306);

$databaseConnection = new mysqli($host, $username, $password, $database, $port);
if ($databaseConnection->connect_errno) {
    throw new RuntimeException('Could not connect to the Polaris database: ' . $databaseConnection->connect_error);
}

$offers = $databaseConnection->query(
    "SELECT offer.id, offer.item_ids
     FROM catalog_items offer
     INNER JOIN catalog_pages page ON page.id = offer.page_id
     WHERE page.visible = '1' AND page.enabled = '1'
     ORDER BY page.order_num, offer.order_number, offer.id"
);
if (!$offers) throw new RuntimeException($databaseConnection->error);

$offerByItem = [];
while ($offer = $offers->fetch_assoc()) {
    foreach (explode(';', $offer['item_ids']) as $itemId) {
        $itemId = (int) trim($itemId);
        if ($itemId > 0 && !isset($offerByItem[$itemId])) $offerByItem[$itemId] = (int) $offer['id'];
    }
}

$document = json_decode(file_get_contents($furniData), true, 512, JSON_THROW_ON_ERROR);
$updated = 0;
$cleared = 0;
foreach (['furnitype', 'wallitemtype'] as $type) {
    if (!isset($document['roomitemtypes'][$type])) continue;
    foreach ($document['roomitemtypes'][$type] as &$item) {
        $itemId = (int) ($item['id'] ?? 0);
        $expectedOfferId = $offerByItem[$itemId] ?? -1;
        if ((int) ($item['offerid'] ?? -1) === $expectedOfferId) continue;
        if ($expectedOfferId === -1) $cleared++;
        $item['offerid'] = $expectedOfferId;
        $updated++;
    }
    unset($item);
}

$temporary = $furniData . '.catalog-sync.tmp';
file_put_contents($temporary, json_encode($document, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR));
if (!rename($temporary, $furniData)) {
    @unlink($temporary);
    throw new RuntimeException('Could not replace FurnitureData.json.');
}

echo json_encode([
    'active_offers' => count($offerByItem),
    'furnidata_entries_updated' => $updated,
    'stale_offerids_cleared' => $cleared
], JSON_UNESCAPED_SLASHES) . PHP_EOL;
