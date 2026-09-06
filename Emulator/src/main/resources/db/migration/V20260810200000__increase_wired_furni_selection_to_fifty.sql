-- BSS-compatible WIRED selectors allow up to fifty selected furni.
INSERT INTO `wired_emulator_settings` (`key`, `value`, `comment`)
VALUES (
    'hotel.wired.furni.selection.count',
    '50',
    'Maximum number of furni that a wired box can store or select.'
)
ON DUPLICATE KEY UPDATE
    `value` = VALUES(`value`),
    `comment` = VALUES(`comment`);
