CREATE TABLE IF NOT EXISTS `wired_emulator_settings` (
  `key` varchar(191) NOT NULL,
  `value` text NOT NULL,
  `comment` text NOT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

INSERT INTO `wired_emulator_settings` (`key`, `value`, `comment`)
SELECT 'wired.engine.enabled', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.engine.enabled' LIMIT 1), '1'), 'Compatibility flag kept for older configs. The runtime now always uses the new wired engine.'
UNION ALL
SELECT 'wired.engine.exclusive', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.engine.exclusive' LIMIT 1), '1'), 'Compatibility flag kept for older configs. The runtime now always uses the new wired engine.'
UNION ALL
SELECT 'wired.engine.maxStepsPerStack', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.engine.maxStepsPerStack' LIMIT 1), '100'), 'Maximum amount of internal processing steps allowed for a single wired stack execution.'
UNION ALL
SELECT 'wired.engine.debug', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.engine.debug' LIMIT 1), '0'), 'Enable verbose debug logging for the new wired engine.'
UNION ALL
SELECT 'wired.custom.enabled', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.custom.enabled' LIMIT 1), '0'), 'Enable custom legacy wired behaviour such as user-based cooldown exceptions and compatibility logic.'
UNION ALL
SELECT 'hotel.wired.furni.selection.count', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'hotel.wired.furni.selection.count' LIMIT 1), '5'), 'Maximum number of furni that a wired box can store or select.'
UNION ALL
SELECT 'hotel.wired.max_delay', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'hotel.wired.max_delay' LIMIT 1), '20'), 'Maximum delay value accepted by wired effects that support delayed execution.'
UNION ALL
SELECT 'hotel.wired.message.max_length', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'hotel.wired.message.max_length' LIMIT 1), '100'), 'Maximum length of text fields used by wired messages and bot text effects.'
UNION ALL
SELECT 'wired.effect.teleport.delay', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.effect.teleport.delay' LIMIT 1), '500'), 'Delay in milliseconds used by wired teleport movement.'
UNION ALL
SELECT 'wired.place.under', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.place.under' LIMIT 1), '0'), 'Allow placing wired furniture underneath other items when room rules permit it.'
UNION ALL
SELECT 'wired.tick.interval.ms', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.tick.interval.ms' LIMIT 1), '50'), 'Global wired tick interval in milliseconds used by repeaters and other tick-driven wired items.'
UNION ALL
SELECT 'wired.tick.resolution', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.tick.resolution' LIMIT 1), '100'), 'Legacy wired tick resolution value kept for compatibility with older wired timing setups.'
UNION ALL
SELECT 'wired.tick.debug', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.tick.debug' LIMIT 1), '0'), 'Enable verbose logging for the wired tick service.'
UNION ALL
SELECT 'wired.tick.thread.priority', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.tick.thread.priority' LIMIT 1), '6'), 'Java thread priority used by the wired tick service.'
UNION ALL
SELECT 'wired.highscores.displaycount', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.highscores.displaycount' LIMIT 1), '25'), 'Maximum number of wired highscore entries shown to users when a highscore is displayed.'
UNION ALL
SELECT 'wired.abuse.max.recursion.depth', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.abuse.max.recursion.depth' LIMIT 1), '10'), 'Maximum recursive wired depth allowed before execution is stopped.'
UNION ALL
SELECT 'wired.abuse.max.events.per.window', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.abuse.max.events.per.window' LIMIT 1), '100'), 'Maximum amount of identical wired events allowed inside the abuse rate-limit window before a room ban is applied.'
UNION ALL
SELECT 'wired.abuse.rate.limit.window.ms', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.abuse.rate.limit.window.ms' LIMIT 1), '10000'), 'Time window in milliseconds used by the wired abuse rate limiter.'
UNION ALL
SELECT 'wired.abuse.ban.duration.ms', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.abuse.ban.duration.ms' LIMIT 1), '600000'), 'Duration in milliseconds of the temporary wired ban after abuse detection.'
UNION ALL
SELECT 'wired.monitor.usage.window.ms', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.usage.window.ms' LIMIT 1), '1000'), 'Rolling window size in milliseconds used to calculate wired usage in the :wired monitor.'
UNION ALL
SELECT 'wired.monitor.usage.limit', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.usage.limit' LIMIT 1), '1000'), 'Maximum wired usage budget allowed in one monitor window before EXECUTION_CAP is raised.'
UNION ALL
SELECT 'wired.monitor.delayed.events.limit', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.delayed.events.limit' LIMIT 1), '100'), 'Maximum number of delayed wired events that can be queued in one room at the same time.'
UNION ALL
SELECT 'wired.monitor.overload.average.ms', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.overload.average.ms' LIMIT 1), '50'), 'Average execution time threshold in milliseconds that starts overload tracking.'
UNION ALL
SELECT 'wired.monitor.overload.peak.ms', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.overload.peak.ms' LIMIT 1), '150'), 'Peak single execution time threshold in milliseconds that starts overload tracking.'
UNION ALL
SELECT 'wired.monitor.overload.consecutive.windows', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.overload.consecutive.windows' LIMIT 1), '2'), 'Number of consecutive overloaded monitor windows required before logging EXECUTOR_OVERLOAD.'
UNION ALL
SELECT 'wired.monitor.heavy.usage.percent', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.heavy.usage.percent' LIMIT 1), '70'), 'Usage percentage threshold that contributes to marking a room as heavy in the :wired monitor.'
UNION ALL
SELECT 'wired.monitor.heavy.consecutive.windows', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.heavy.consecutive.windows' LIMIT 1), '5'), 'Number of consecutive windows above the heavy usage threshold required before the room is marked as heavy.'
UNION ALL
SELECT 'wired.monitor.heavy.delayed.percent', COALESCE((SELECT `value` FROM `emulator_settings` WHERE `key` = 'wired.monitor.heavy.delayed.percent' LIMIT 1), '60'), 'Delayed queue percentage threshold that also contributes to the heavy-room calculation.'
ON DUPLICATE KEY UPDATE
  `value` = VALUES(`value`),
  `comment` = VALUES(`comment`);

DELETE FROM `emulator_settings`
WHERE `key` IN (
  'wired.engine.enabled',
  'wired.engine.exclusive',
  'wired.engine.maxStepsPerStack',
  'wired.engine.debug',
  'wired.custom.enabled',
  'hotel.wired.furni.selection.count',
  'hotel.wired.max_delay',
  'hotel.wired.message.max_length',
  'wired.effect.teleport.delay',
  'wired.place.under',
  'wired.tick.interval.ms',
  'wired.tick.resolution',
  'wired.tick.debug',
  'wired.tick.thread.priority',
  'wired.highscores.displaycount',
  'wired.abuse.max.recursion.depth',
  'wired.abuse.max.events.per.window',
  'wired.abuse.rate.limit.window.ms',
  'wired.abuse.ban.duration.ms',
  'wired.monitor.usage.window.ms',
  'wired.monitor.usage.limit',
  'wired.monitor.delayed.events.limit',
  'wired.monitor.overload.average.ms',
  'wired.monitor.overload.peak.ms',
  'wired.monitor.overload.consecutive.windows',
  'wired.monitor.heavy.usage.percent',
  'wired.monitor.heavy.consecutive.windows',
  'wired.monitor.heavy.delayed.percent'
);
