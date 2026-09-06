-- Global chat must remain uncensored. Room-specific filters and moderation
-- controls keep their schema, but the hotel-wide room/messenger filter is off.
UPDATE `emulator_settings`
SET `value` = '0'
WHERE `key` IN (
    'hotel.wordfilter.enabled',
    'hotel.wordfilter.rooms',
    'hotel.wordfilter.messenger'
);
