-- Wired Abuse Protection Settings
-- These settings control the wired abuse detection and rate limiting system

-- Maximum recursion depth to prevent infinite loops (e.g., collision + chase triggering each other)
INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('wired.abuse.max.recursion.depth', '10');

-- Maximum events of same type per room within the rate limit window before triggering a ban
-- Set higher for rooms with many users and complex wired setups
INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('wired.abuse.max.events.per.window', '100');

-- Time window in milliseconds for counting rapid events
-- Events are counted within this window to detect abuse patterns
INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('wired.abuse.rate.limit.window.ms', '10000');

-- Duration in milliseconds to ban wired execution in a room after abuse is detected
-- Default: 600000 (10 minutes)
INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('wired.abuse.ban.duration.ms', '600000');

-- Wired Abuse Alert Texts
-- Alert shown to all users in the room when wired is temporarily disabled
INSERT INTO `emulator_texts` (`key`, `value`) VALUES ('wired.abuse.room.alert', 'Wired execution has been temporarily disabled in this room due to abuse detection. It will resume in %minutes% minutes.');

-- Title for the staff bubble alert
INSERT INTO `emulator_texts` (`key`, `value`) VALUES ('wired.abuse.staff.title', 'Wired Abuse Detected');

-- Message for the staff bubble alert
-- Available placeholders: %roomname%, %owner%, %minutes%
INSERT INTO `emulator_texts` (`key`, `value`) VALUES ('wired.abuse.staff.message', 'Room: %roomname%\nOwner: %owner%\nBanned for %minutes% minutes.');

-- Link text for the staff bubble alert (navigates to the room)
INSERT INTO `emulator_texts` (`key`, `value`) VALUES ('wired.abuse.staff.link', 'Go to Room');

-- Default tick resolution for wired timer triggers (in milliseconds)
INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('wired.tick.resolution', '100');


-- =====================================================
-- Wired Engine Rewrite - Configuration Settings
-- =====================================================
-- This SQL script adds the configuration options for the new 
-- context-driven wired engine architecture.
-- 
-- Run this script after upgrading to enable the new wired system.
-- =====================================================

-- Insert new wired engine configuration settings
INSERT INTO `emulator_settings` (`key`, `value`) VALUES
('wired.engine.enabled', '0'),
('wired.engine.exclusive', '0'),
('wired.engine.maxStepsPerStack', '100'),
('wired.engine.debug', '0')
ON DUPLICATE KEY UPDATE `key` = `key`;

-- =====================================================
-- Configuration Options Explained:
-- =====================================================
-- 
-- wired.engine.enabled
--   Enable the new wired engine. When set to 1, the new engine
--   runs alongside the legacy WiredHandler for parallel testing.
--   Default: 0 (disabled)
--
-- wired.engine.exclusive
--   When set to 1, disables the legacy WiredHandler completely.
--   Only enable this after thorough testing with parallel mode.
--   Default: 0 (legacy handler still active)
--
-- wired.engine.maxStepsPerStack
--   Maximum number of steps (trigger checks + condition evaluations
--   + effect executions) allowed per wired stack execution.
--   Prevents infinite loops from misconfigured wired setups.
--   Default: 100
--
-- wired.engine.debug
--   Enable verbose debug logging for wired execution.
--   Useful for troubleshooting wired stack behavior.
--   Default: 0 (disabled)
--
-- =====================================================
-- Migration Path:
-- =====================================================
-- 
-- Phase 1: Parallel Testing
--   UPDATE emulator_settings SET value = '1' WHERE `key` = 'wired.engine.enabled';
--   -- Test all wired functionality, compare behavior between old and new
--
-- Phase 2: Switch to New Engine
--   UPDATE emulator_settings SET value = '1' WHERE `key` = 'wired.engine.exclusive';
--   -- Legacy handler disabled, new engine handles all wired events
--
-- Phase 3: Cleanup (after confirming stability)
--   -- Remove legacy WiredHandler calls from codebase
--
-- =====================================================


-- =====================================================
-- Wired Tick System - Configuration Settings
-- =====================================================
-- This SQL script adds the configuration options for the new 
-- high-resolution wired tick system (50ms default).
-- 
-- Run this script to configure the wired timer triggers.
-- =====================================================

-- Insert new wired tick system configuration settings
INSERT INTO `emulator_settings` (`key`, `value`) VALUES
('wired.tick.interval.ms', '50'),
('wired.tick.debug', '0'),
('wired.tick.thread.priority', '6')
ON DUPLICATE KEY UPDATE `key` = `key`;

-- =====================================================
-- Configuration Options Explained:
-- =====================================================
-- 
-- wired.tick.interval.ms
--   The tick interval in milliseconds for wired timer triggers.
--   Lower values = more precise timing but higher CPU usage.
--   Recommended: 50 (default), Range: 10-500
--   Default: 50
--
-- wired.tick.debug
--   Enable verbose debug logging for wired tick operations.
--   Logs each tick cycle and trigger execution.
--   Warning: Very verbose, only enable for troubleshooting.
--   Default: 0 (disabled)
--
-- wired.tick.thread.priority
--   Thread priority for the wired tick service (1-10).
--   Higher priority = better timing accuracy under load.
--   Java Thread priorities: MIN=1, NORM=5, MAX=10
--   Default: 6 (slightly above normal)
--
-- =====================================================
-- Usage Examples:
-- =====================================================
-- 
-- Increase tick resolution for competitive mini-games:
--   UPDATE emulator_settings SET value = '25' WHERE `key` = 'wired.tick.interval.ms';
--
-- Reduce CPU usage on low-end servers:
--   UPDATE emulator_settings SET value = '100' WHERE `key` = 'wired.tick.interval.ms';
--
-- Enable debug logging for troubleshooting:
--   UPDATE emulator_settings SET value = '1' WHERE `key` = 'wired.tick.debug';
--
-- =====================================================
INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('pathfinder.click.delay', '0');
INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('pathfinder.retro-style.diagonals', '0');
INSERT INTO `emulator_settings` (`key`, `value`) VALUES ('pathfinder.step.allow.falling', '1');

