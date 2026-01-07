package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectGiveReward;
import com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTriggerStacks;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredGiveRewardItem;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.habbohotel.wired.migrate.WiredEvents;
import com.eu.habbo.habbohotel.wired.tick.WiredTickService;
import com.eu.habbo.habbohotel.wired.tick.WiredTickable;
import com.eu.habbo.messages.outgoing.catalog.PurchaseOKComposer;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.users.AddUserBadgeComposer;
import com.eu.habbo.messages.outgoing.wired.WiredRewardAlertComposer;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.users.UserWiredRewardReceived;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Manager class for the new wired engine system.
 * <p>
 * This class serves as the integration point between the emulator and the new
 * wired engine. It provides static methods for triggering events and manages
 * the lifecycle of the engine.
 * </p>
 * 
 * <h3>Configuration Options:</h3>
 * <ul>
 *   <li>{@code wired.engine.enabled} - Enable new engine (parallel mode)</li>
 *   <li>{@code wired.engine.exclusive} - Disable legacy handler when true</li>
 *   <li>{@code wired.engine.maxStepsPerStack} - Loop protection limit</li>
 *   <li>{@code wired.engine.debug} - Verbose logging</li>
 * </ul>
 * 
 * <h3>Migration Strategy:</h3>
 * <ol>
 *   <li>Set {@code wired.engine.enabled=true} to run both engines in parallel</li>
 *   <li>Test thoroughly to ensure identical behavior</li>
 *   <li>Set {@code wired.engine.exclusive=true} to disable legacy engine</li>
 *   <li>Full migration complete - WiredManager is now the only wired engine</li>
 * </ol>
 * 
 * @see WiredEngine
 * @see WiredEvents
 */
public final class WiredManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WiredManager.class);

    // Configuration keys
    public static final String CONFIG_ENABLED = "wired.engine.enabled";
    public static final String CONFIG_EXCLUSIVE = "wired.engine.exclusive";
    public static final String CONFIG_MAX_STEPS = "wired.engine.maxStepsPerStack";
    public static final String CONFIG_DEBUG = "wired.engine.debug";

    // Defaults
    private static final boolean DEFAULT_ENABLED = false;
    private static final boolean DEFAULT_EXCLUSIVE = false;
    private static final int DEFAULT_MAX_STEPS = 100;

    /** The singleton engine instance */
    private static volatile WiredEngine engine;
    
    /** The stack index */
    private static volatile RoomWiredStackIndex stackIndex;
    
    /** Whether the engine is initialized */
    private static volatile boolean initialized = false;

    private WiredManager() {
        // Static utility class
    }
    /**
     * Event handler called when the emulator is loaded.
     * Initializes the wired manager.
     */
    @EventHandler
    public static void onEmulatorLoaded(EmulatorLoadedEvent event) {
        initialize();
    }

    /**
     * Initialize the wired manager and engine.
     * Called during emulator startup.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        LOGGER.info("Initializing Wired Manager...");

        // Load configuration
        boolean enabled = Emulator.getConfig().getBoolean(CONFIG_ENABLED, DEFAULT_ENABLED);
        int maxSteps = Emulator.getConfig().getInt(CONFIG_MAX_STEPS, DEFAULT_MAX_STEPS);
        boolean debug = Emulator.getConfig().getBoolean(CONFIG_DEBUG, false);
        
        // Load additional configuration
        MAXIMUM_FURNI_SELECTION = Emulator.getConfig().getInt("hotel.wired.furni.selection.count", 5);
        TELEPORT_DELAY = Emulator.getConfig().getInt("wired.effect.teleport.delay", 500);

        // Set debug mode
        if (debug) {
            setDebugEnabled(true);
        }

        // Create components
        stackIndex = new RoomWiredStackIndex();
        WiredServices services = DefaultWiredServices.getInstance();
        engine = new WiredEngine(services, stackIndex, maxSteps);
        
        // Start the centralized tick service (50ms interval)
        WiredTickService.getInstance().start();

        initialized = true;
        
        LOGGER.info("Wired Manager initialized - enabled: {}, maxSteps: {}, debug: {}", 
                enabled, maxSteps, debug);
    }

    /**
     * Shutdown the wired manager.
     * Called during emulator shutdown.
     */
    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        LOGGER.info("Shutting down Wired Manager...");
        
        // Stop the tick service first
        WiredTickService.getInstance().stop();
        
        if (stackIndex != null) {
            stackIndex.clearAll();
        }
        
        if (engine != null) {
            engine.clearUnseenCache();
        }

        initialized = false;
        LOGGER.info("Wired Manager shutdown complete");
    }

    /**
     * Check if the new wired engine is enabled.
     * @return true if enabled
     */
    public static boolean isEnabled() {
        return Emulator.getConfig().getBoolean(CONFIG_ENABLED, DEFAULT_ENABLED);
    }

    /**
     * Check if the new engine is exclusive (legacy disabled).
     * @return true if exclusive mode
     */
    public static boolean isExclusive() {
        return Emulator.getConfig().getBoolean(CONFIG_EXCLUSIVE, DEFAULT_EXCLUSIVE);
    }

    /**
     * Get the wired engine instance.
     * @return the engine, or null if not initialized
     */
    public static WiredEngine getEngine() {
        return engine;
    }

    /**
     * Get the stack index instance.
     * @return the stack index, or null if not initialized
     */
    public static RoomWiredStackIndex getStackIndex() {
        return stackIndex;
    }

    // ========== Event Triggering Methods ==========

    /**
     * Handle a wired event using the new engine.
     * @param event the event to handle
     * @return true if any stack was triggered
     */
    public static boolean handleEvent(WiredEvent event) {
        if (!isEnabled() || engine == null) {
            return false;
        }
        
        return engine.handleEvent(event);
    }

    /**
     * Trigger when a user walks onto furniture.
     */
    public static boolean triggerUserWalksOn(Room room, RoomUnit user, HabboItem item) {
        if (!isEnabled() || room == null || user == null || item == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.userWalksOn(room, user, item);
        return handleEvent(event);
    }

    /**
     * Trigger when a user walks off furniture.
     */
    public static boolean triggerUserWalksOff(Room room, RoomUnit user, HabboItem item) {
        if (!isEnabled() || room == null || user == null || item == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.userWalksOff(room, user, item);
        return handleEvent(event);
    }

    /**
     * Trigger when a user says something.
     */
    public static boolean triggerUserSays(Room room, RoomUnit user, String message) {
        if (!isEnabled() || room == null || user == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.userSays(room, user, message);
        return handleEvent(event);
    }

    /**
     * Trigger when a user enters the room.
     */
    public static boolean triggerUserEntersRoom(Room room, RoomUnit user) {
        if (!isEnabled() || room == null || user == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.userEntersRoom(room, user);
        return handleEvent(event);
    }

    /**
     * Trigger when furniture state changes.
     */
    public static boolean triggerFurniStateChanged(Room room, RoomUnit user, HabboItem item) {
        if (!isEnabled() || room == null || item == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.furniStateChanged(room, user, item);
        return handleEvent(event);
    }

    /**
     * Trigger a timer tick.
     */
    public static boolean triggerTimerTick(Room room, HabboItem timerItem) {
        if (!isEnabled() || room == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.timerTick(room, timerItem);
        return handleEvent(event);
    }

    /**
     * Trigger a periodic timer.
     */
    public static boolean triggerTimerRepeat(Room room, HabboItem timerItem) {
        if (!isEnabled() || room == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.timerRepeat(room, timerItem);
        return handleEvent(event);
    }

    /**
     * Trigger game start.
     */
    public static boolean triggerGameStarts(Room room) {
        if (!isEnabled() || room == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.gameStarts(room);
        return handleEvent(event);
    }

    /**
     * Trigger game end.
     */
    public static boolean triggerGameEnds(Room room) {
        if (!isEnabled() || room == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.gameEnds(room);
        return handleEvent(event);
    }

    /**
     * Trigger bot collision.
     */
    public static boolean triggerBotCollision(Room room, RoomUnit botUnit) {
        if (!isEnabled() || room == null || botUnit == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.botCollision(room, botUnit);
        return handleEvent(event);
    }

    /**
     * Trigger when bot reaches furniture.
     */
    public static boolean triggerBotReachedFurni(Room room, RoomUnit botUnit, HabboItem item) {
        if (!isEnabled() || room == null || botUnit == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.botReachedFurni(room, botUnit, item);
        return handleEvent(event);
    }

    /**
     * Trigger when bot reaches a habbo.
     */
    public static boolean triggerBotReachedHabbo(Room room, RoomUnit botUnit, RoomUnit targetUser) {
        if (!isEnabled() || room == null || botUnit == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.botReachedHabbo(room, botUnit, targetUser);
        return handleEvent(event);
    }

    /**
     * Trigger when score is achieved.
     */
    public static boolean triggerScoreAchieved(Room room, RoomUnit user, int score) {
        if (!isEnabled() || room == null || user == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.scoreAchieved(room, user, score);
        return handleEvent(event);
    }

    /**
     * Trigger when user starts idling.
     */
    public static boolean triggerUserIdles(Room room, RoomUnit user) {
        if (!isEnabled() || room == null || user == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.userIdles(room, user);
        return handleEvent(event);
    }

    /**
     * Trigger when user stops idling.
     */
    public static boolean triggerUserUnidles(Room room, RoomUnit user) {
        if (!isEnabled() || room == null || user == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.userUnidles(room, user);
        return handleEvent(event);
    }

    /**
     * Trigger when user starts dancing.
     */
    public static boolean triggerUserStartsDancing(Room room, RoomUnit user) {
        if (!isEnabled() || room == null || user == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.userStartsDancing(room, user);
        return handleEvent(event);
    }

    /**
     * Trigger when user stops dancing.
     */
    public static boolean triggerUserStopsDancing(Room room, RoomUnit user) {
        if (!isEnabled() || room == null || user == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.userStopsDancing(room, user);
        return handleEvent(event);
    }

    /**
     * Trigger when a team wins a game.
     */
    public static boolean triggerTeamWins(Room room, RoomUnit user) {
        if (!isEnabled() || room == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.teamWins(room, user);
        return handleEvent(event);
    }

    /**
     * Trigger when a team loses a game.
     */
    public static boolean triggerTeamLoses(Room room, RoomUnit user) {
        if (!isEnabled() || room == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.teamLoses(room, user);
        return handleEvent(event);
    }

    /**
     * Trigger from legacy system for parallel running.
     * This allows the new engine to run alongside the old one during migration.
     */
    public static boolean triggerFromLegacy(WiredTriggerType triggerType, RoomUnit roomUnit, Room room, Object[] stuff) {
        if (!isEnabled() || room == null) {
            return false;
        }
        
        WiredEvent event = WiredEvents.fromLegacy(triggerType, room, roomUnit, stuff);
        return handleEvent(event);
    }

    // ========== Index Management ==========

    /**
     * Invalidate the wired index for a room.
     * Call this when wired items are added/removed/moved.
     */
    public static void invalidateRoom(Room room) {
        if (stackIndex != null && room != null) {
            stackIndex.invalidateAll(room);
            if (debugEnabled) {
                LOGGER.info("[Wired] Cache invalidated for room {}", room.getId());
            }
        }
    }

    /**
     * Invalidate the wired index for a specific tile.
     */
    public static void invalidateTile(Room room, RoomTile tile) {
        if (stackIndex != null && room != null && tile != null) {
            stackIndex.invalidate(room, tile);
        }
    }

    /**
     * Rebuild the wired index for a room.
     */
    public static void rebuildRoom(Room room) {
        if (stackIndex != null && room != null) {
            stackIndex.rebuild(room);
        }
    }

    // ========== Configuration Constants (moved from WiredHandler) ==========

    /** Maximum number of furniture items that can be selected in a single wired component */
    public static int MAXIMUM_FURNI_SELECTION = 5;
    
    /** Delay in milliseconds between teleport executions */
    public static int TELEPORT_DELAY = 500;

    // ========== Debug Mode ==========
    
    /** Debug mode - when enabled, logs detailed wired execution flow */
    private static boolean debugEnabled = false;

    /**
     * Enables or disables wired debug mode.
     * When enabled, detailed execution logs are written to help troubleshoot wired stacks.
     * 
     * @param enabled true to enable debug logging, false to disable
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        if (enabled) {
            LOGGER.info("Wired debug mode ENABLED");
        }
    }
    
    /**
     * Checks if wired debug mode is enabled.
     * 
     * @return true if debug mode is active
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * Logs a debug message if debug mode is enabled.
     * 
     * @param message the message to log
     * @param args optional format arguments
     */
    public static void debug(String message, Object... args) {
        if (debugEnabled) {
            LOGGER.info("[WIRED DEBUG] " + message, args);
        }
    }

    // ========== JSON Utilities ==========
    
    private static GsonBuilder gsonBuilder = null;
    private static Gson cachedGson = null;

    public static GsonBuilder getGsonBuilder() {
        if (gsonBuilder == null) {
            gsonBuilder = new GsonBuilder();
        }
        return gsonBuilder;
    }
    
    /**
     * Gets a cached Gson instance. This is more efficient than calling
     * getGsonBuilder().create() multiple times, as Gson instances are thread-safe
     * and can be reused.
     * 
     * @return a cached Gson instance
     */
    public static Gson getGson() {
        if (cachedGson == null) {
            cachedGson = getGsonBuilder().create();
        }
        return cachedGson;
    }

    // ========== Tick Service Integration ==========
    
    /**
     * Registers a tickable wired item with the centralized tick service.
     * <p>
     * Call this when a time-based wired trigger is placed in a room or when
     * a room is loaded.
     * </p>
     * 
     * @param room the room the item is in
     * @param tickable the tickable item (e.g., WiredTriggerRepeater)
     */
    public static void registerTickable(Room room, WiredTickable tickable) {
        WiredTickService.getInstance().register(room, tickable);
    }
    
    /**
     * Unregisters a tickable wired item from the tick service.
     * <p>
     * Call this when a time-based wired trigger is picked up or when
     * a room is unloaded.
     * </p>
     * 
     * @param room the room the item was in
     * @param tickable the tickable item
     */
    public static void unregisterTickable(Room room, WiredTickable tickable) {
        WiredTickService.getInstance().unregister(room, tickable);
    }
    
    /**
     * Unregisters all tickables for a room.
     * <p>
     * Call this when a room is unloaded to clean up all tick registrations.
     * </p>
     * 
     * @param room the room
     */
    public static void unregisterRoomTickables(Room room) {
        WiredTickService.getInstance().unregisterRoom(room);
    }
    
    /**
     * Gets the tick service instance.
     * 
     * @return the WiredTickService
     */
    public static WiredTickService getTickService() {
        return WiredTickService.getInstance();
    }

    // ========== Timer Management ==========

    /**
     * Resets all wired timers in a room.
     * <p>
     * This uses the new tick service for managing timer resets.
     * </p>
     * 
     * @param room the room
     */
    public static void resetTimers(Room room) {
        if (!room.isLoaded())
            return;

        // Use the centralized tick service for timer resets
        WiredTickService.getInstance().resetRoomTimers(room);

        room.setLastTimerReset(Emulator.getIntUnixTimestamp());
    }

    // ========== Effect Execution ==========

    public static boolean executeEffectsAtTiles(THashSet<RoomTile> tiles, final RoomUnit roomUnit, final Room room, final Object[] stuff) {
        for (RoomTile tile : tiles) {
            if (room != null) {
                THashSet<HabboItem> items = room.getItemsAt(tile);

                long millis = room.getCycleTimestamp();
                for (final HabboItem item : items) {
                    if (item instanceof InteractionWiredEffect && !(item instanceof WiredEffectTriggerStacks)) {
                        InteractionWiredEffect effect = (InteractionWiredEffect) item;
                        WiredEvent event = WiredEvent.builder(WiredEvent.Type.CUSTOM, room)
                            .actor(roomUnit)
                            .legacyStuff(stuff)
                            .build();
                        WiredContext ctx = new WiredContext(event, effect, DefaultWiredServices.getInstance(), new WiredState(100));
                        effect.execute(ctx);
                        effect.setCooldown(millis);
                    }
                }
            }
        }

        return true;
    }

    // ========== Reward System ==========

    /**
     * Asynchronously drops/deletes all rewards given by a specific wired item.
     * Used when a wired reward box is picked up or reset.
     * 
     * @param wiredId The ID of the wired item whose rewards should be deleted
     */
    public static void dropRewards(int wiredId) {
        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); 
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM wired_rewards_given WHERE wired_item = ?")) {
                statement.setInt(1, wiredId);
                statement.execute();
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        });
    }

    private static void giveReward(Habbo habbo, WiredEffectGiveReward wiredBox, WiredGiveRewardItem reward) {
        if (wiredBox.getLimit() > 0)
            wiredBox.incrementGiven();

        final int wiredId = wiredBox.getId();
        final int habboId = habbo.getHabboInfo().getId();
        final int rewardId = reward.id;
        final int timestamp = Emulator.getIntUnixTimestamp();
        
        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); 
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO wired_rewards_given (wired_item, user_id, reward_id, timestamp) VALUES ( ?, ?, ?, ?)")) {
                statement.setInt(1, wiredId);
                statement.setInt(2, habboId);
                statement.setInt(3, rewardId);
                statement.setInt(4, timestamp);
                statement.execute();
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        });

        if (reward.badge) {
            UserWiredRewardReceived rewardReceived = new UserWiredRewardReceived(habbo, wiredBox, "badge", reward.data);
            if (Emulator.getPluginManager().fireEvent(rewardReceived).isCancelled())
                return;

            if (rewardReceived.value.isEmpty())
                return;
            
            if (habbo.getInventory().getBadgesComponent().hasBadge(rewardReceived.value))
                return;

            HabboBadge badge = new HabboBadge(0, rewardReceived.value, 0, habbo);
            Emulator.getThreading().run(badge);
            habbo.getInventory().getBadgesComponent().addBadge(badge);
            habbo.getClient().sendResponse(new AddUserBadgeComposer(badge));
            habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_RECEIVED_BADGE));
        } else {
            String[] data = reward.data.split("#");

            if (data.length == 2) {
                UserWiredRewardReceived rewardReceived = new UserWiredRewardReceived(habbo, wiredBox, data[0], data[1]);
                if (Emulator.getPluginManager().fireEvent(rewardReceived).isCancelled())
                    return;

                if (rewardReceived.value.isEmpty())
                    return;

                if (rewardReceived.type.equalsIgnoreCase("credits")) {
                    int credits = Integer.parseInt(rewardReceived.value);
                    habbo.giveCredits(credits);
                } else if (rewardReceived.type.equalsIgnoreCase("pixels")) {
                    int pixels = Integer.parseInt(rewardReceived.value);
                    habbo.givePixels(pixels);
                } else if (rewardReceived.type.startsWith("points")) {
                    int points = Integer.parseInt(rewardReceived.value);
                    int type = 5;

                    try {
                        type = Integer.parseInt(rewardReceived.type.replace("points", ""));
                    } catch (Exception e) {
                    }

                    habbo.givePoints(type, points);
                } else if (rewardReceived.type.equalsIgnoreCase("furni")) {
                    Item baseItem = Emulator.getGameEnvironment().getItemManager().getItem(Integer.parseInt(rewardReceived.value));
                    if (baseItem != null) {
                        HabboItem item = Emulator.getGameEnvironment().getItemManager().createItem(habbo.getHabboInfo().getId(), baseItem, 0, 0, "");

                        if (item != null) {
                            habbo.getClient().sendResponse(new AddHabboItemComposer(item));
                            habbo.getClient().getHabbo().getInventory().getItemsComponent().addItem(item);
                            habbo.getClient().sendResponse(new PurchaseOKComposer(null));
                            habbo.getClient().sendResponse(new InventoryRefreshComposer());
                            habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_RECEIVED_ITEM));
                        }
                    }
                } else if (rewardReceived.type.equalsIgnoreCase("respect")) {
                    habbo.getHabboStats().respectPointsReceived += Integer.parseInt(rewardReceived.value);
                } else if (rewardReceived.type.equalsIgnoreCase("cata")) {
                    CatalogItem item = Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(Integer.parseInt(rewardReceived.value));

                    if (item != null) {
                        Emulator.getGameEnvironment().getCatalogManager().purchaseItem(null, item, habbo, 1, "", true);
                    }
                    habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_RECEIVED_ITEM));
                }
            }
        }
    }

    public static boolean getReward(Habbo habbo, WiredEffectGiveReward wiredBox) {
        if (wiredBox.getLimit() > 0) {
            if (wiredBox.getLimit() - wiredBox.getGiven() == 0) {
                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.LIMITED_NO_MORE_AVAILABLE));
                return false;
            }
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as row_count, wired_rewards_given.* FROM wired_rewards_given WHERE user_id = ? AND wired_item = ? ORDER BY timestamp DESC LIMIT ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            statement.setInt(1, habbo.getHabboInfo().getId());
            statement.setInt(2, wiredBox.getId());
            statement.setInt(3, wiredBox.getRewardItems().size());

            try (ResultSet set = statement.executeQuery()) {
                if (set.first()) {
                    if (set.getInt("row_count") >= 1) {
                        if (wiredBox.getRewardTime() == WiredEffectGiveReward.LIMIT_ONCE) {
                            habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALREADY_RECEIVED));
                            return false;
                        }
                    }

                    set.beforeFirst();
                    if (set.next()) {
                        if (wiredBox.getRewardTime() == WiredEffectGiveReward.LIMIT_N_MINUTES) {
                            if (Emulator.getIntUnixTimestamp() - set.getInt("timestamp") <= 60) {
                                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALREADY_RECEIVED_THIS_MINUTE));
                                return false;
                            }
                        }

                        if (wiredBox.isUniqueRewards()) {
                            if (set.getInt("row_count") == wiredBox.getRewardItems().size()) {
                                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALL_COLLECTED));
                                return false;
                            }
                        }

                        if (wiredBox.getRewardTime() == WiredEffectGiveReward.LIMIT_N_HOURS) {
                            if (!(Emulator.getIntUnixTimestamp() - set.getInt("timestamp") >= (3600 * wiredBox.getLimitationInterval()))) {
                                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALREADY_RECEIVED_THIS_HOUR));
                                return false;
                            }
                        }

                        if (wiredBox.getRewardTime() == WiredEffectGiveReward.LIMIT_N_DAY) {
                            if (!(Emulator.getIntUnixTimestamp() - set.getInt("timestamp") >= (86400 * wiredBox.getLimitationInterval()))) {
                                habbo.getClient().sendResponse(new WiredRewardAlertComposer(WiredRewardAlertComposer.REWARD_ALREADY_RECEIVED_THIS_TODAY));
                                return false;
                            }
                        }
                    }

                    if (wiredBox.isUniqueRewards()) {
                        for (WiredGiveRewardItem item : wiredBox.getRewardItems()) {
                            set.beforeFirst();
                            boolean found = false;

                            while (set.next()) {
                                if (set.getInt("reward_id") == item.id)
                                    found = true;
                            }

                            if (!found) {
                                giveReward(habbo, wiredBox, item);
                                return true;
                            }
                        }
                    } else {
                        int randomNumber = Emulator.getRandom().nextInt(101);

                        int count = 0;
                        for (WiredGiveRewardItem item : wiredBox.getRewardItems()) {
                            if (randomNumber >= count && randomNumber <= (count + item.probability)) {
                                giveReward(habbo, wiredBox, item);
                                return true;
                            }

                            count += item.probability;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return false;
    }
}

