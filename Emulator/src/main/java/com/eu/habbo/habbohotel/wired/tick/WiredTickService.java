package com.eu.habbo.habbohotel.wired.tick;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized tick service for all wired timing operations.
 * <p>
 * This service runs a single 50ms tick loop that processes all registered
 * {@link WiredTickable} items across all rooms. This replaces the old
 * per-room 500ms cycle approach and provides:
 * </p>
 * 
 * <ul>
 *   <li>Higher resolution timing (50ms vs 500ms)</li>
 *   <li>Centralized management - single thread for all rooms</li>
 *   <li>Proper room lifecycle handling</li>
 *   <li>Efficient registration/unregistration</li>
 * </ul>
 * 
 * <h3>Architecture:</h3>
 * <pre>
 * WiredTickService (singleton)
 *   └── ScheduledExecutorService (50ms tick)
 *         └── For each room with tickables:
 *               └── For each WiredTickable:
 *                     └── onWiredTick(room, currentTime)
 * </pre>
 * 
 * <h3>Thread Safety:</h3>
 * All collections are thread-safe. The tick loop catches and logs exceptions
 * to prevent one bad item from crashing the entire service.
 * 
 * @see WiredTickable
 */
public final class WiredTickService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredTickService.class);
    
    /** Default tick interval in milliseconds */
    public static final int DEFAULT_TICK_INTERVAL_MS = 50;
    
    /** Minimum allowed tick interval (prevents CPU overload) */
    public static final int MIN_TICK_INTERVAL_MS = 10;
    
    /** Maximum allowed tick interval */
    public static final int MAX_TICK_INTERVAL_MS = 500;
    
    /** Singleton instance */
    private static volatile WiredTickService instance;
    
    /** The configured tick interval in milliseconds */
    private int tickIntervalMs = DEFAULT_TICK_INTERVAL_MS;
    
    /** Whether debug logging is enabled */
    private boolean debugEnabled = false;
    
    /** Thread priority for the tick service */
    private int threadPriority = Thread.NORM_PRIORITY + 1;
    
    /** 
     * Global tick counter - increments every tick.
     * All repeaters use this to stay synchronized.
     * Repeaters fire when (tickCount * tickIntervalMs) % repeatTime == 0
     */
    private volatile long tickCount = 0;
    
    /** The scheduled executor for the tick loop */
    private ScheduledExecutorService scheduler;
    
    /** The scheduled future for the tick task */
    private ScheduledFuture<?> tickTask;
    
    /** Map of room ID to set of registered tickables */
    private final ConcurrentHashMap<Integer, Set<WiredTickable>> roomTickables;
    
    /** Whether the service is running */
    private final AtomicBoolean running;
    
    /**
     * Private constructor for singleton.
     */
    private WiredTickService() {
        this.roomTickables = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
    }
    
    /**
     * Loads configuration from emulator settings.
     */
    private void loadConfiguration() {
        // Load tick interval
        int configuredInterval = Emulator.getConfig().getInt("wired.tick.interval.ms", DEFAULT_TICK_INTERVAL_MS);
        this.tickIntervalMs = Math.max(MIN_TICK_INTERVAL_MS, Math.min(MAX_TICK_INTERVAL_MS, configuredInterval));
        
        if (configuredInterval != this.tickIntervalMs) {
            LOGGER.warn("wired.tick.interval.ms value {} is out of range [{}-{}], using {}", 
                configuredInterval, MIN_TICK_INTERVAL_MS, MAX_TICK_INTERVAL_MS, this.tickIntervalMs);
        }
        
        // Load debug flag
        this.debugEnabled = Emulator.getConfig().getBoolean("wired.tick.debug", false);
        
        // Load thread priority
        int configuredPriority = Emulator.getConfig().getInt("wired.tick.thread.priority", Thread.NORM_PRIORITY + 1);
        this.threadPriority = Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY, configuredPriority));
    }
    
    /**
     * Gets the configured tick interval in milliseconds.
     * 
     * @return the tick interval
     */
    public int getTickIntervalMs() {
        return tickIntervalMs;
    }
    
    /**
     * Checks if debug logging is enabled.
     * 
     * @return true if debug is enabled
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return the WiredTickService instance
     */
    public static WiredTickService getInstance() {
        if (instance == null) {
            synchronized (WiredTickService.class) {
                if (instance == null) {
                    instance = new WiredTickService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Starts the tick service.
     * <p>
     * Should be called during emulator startup after WiredManager.initialize().
     * </p>
     */
    public synchronized void start() {
        if (running.get()) {
            LOGGER.warn("WiredTickService already running");
            return;
        }
        
        // Load configuration from emulator settings
        loadConfiguration();
        
        LOGGER.info("Starting WiredTickService with {}ms tick interval (debug={}, priority={})...", 
            tickIntervalMs, debugEnabled, threadPriority);
        
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WiredTickService");
            t.setDaemon(true);
            t.setPriority(threadPriority);
            return t;
        });
        
        this.tickTask = scheduler.scheduleAtFixedRate(
            this::tick,
            tickIntervalMs,
            tickIntervalMs,
            TimeUnit.MILLISECONDS
        );
        
        running.set(true);
        LOGGER.info("WiredTickService started successfully");
    }
    
    /**
     * Stops the tick service.
     * <p>
     * Should be called during emulator shutdown.
     * </p>
     */
    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        
        LOGGER.info("Stopping WiredTickService...");
        
        running.set(false);
        
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        
        roomTickables.clear();
        LOGGER.info("WiredTickService stopped");
    }
    
    /**
     * Checks if the service is running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Registers a tickable item with the service.
     * <p>
     * The item will start receiving {@link WiredTickable#onWiredTick} calls
     * on the next tick cycle.
     * </p>
     * 
     * @param room the room the item is in
     * @param tickable the tickable item
     */
    public void register(Room room, WiredTickable tickable) {
        if (room == null || tickable == null) {
            return;
        }
        
        int roomId = room.getId();
        Set<WiredTickable> tickables = roomTickables.computeIfAbsent(
            roomId, 
            k -> ConcurrentHashMap.newKeySet()
        );
        
        if (tickables.add(tickable)) {
            tickable.onRegistered(room, System.currentTimeMillis());
            LOGGER.debug("Registered tickable {} in room {}", tickable.getId(), roomId);
        }
    }
    
    /**
     * Unregisters a tickable item from the service.
     * 
     * @param room the room the item was in
     * @param tickable the tickable item
     */
    public void unregister(Room room, WiredTickable tickable) {
        if (room == null || tickable == null) {
            return;
        }
        
        int roomId = room.getId();
        Set<WiredTickable> tickables = roomTickables.get(roomId);
        
        if (tickables != null) {
            if (tickables.remove(tickable)) {
                tickable.onUnregistered(room);
                LOGGER.debug("Unregistered tickable {} from room {}", tickable.getId(), roomId);
            }
            
            // Clean up empty sets
            if (tickables.isEmpty()) {
                roomTickables.remove(roomId);
            }
        }
    }
    
    /**
     * Unregisters a tickable by ID.
     * 
     * @param roomId the room ID
     * @param tickableId the tickable item ID
     */
    public void unregister(int roomId, int tickableId) {
        Set<WiredTickable> tickables = roomTickables.get(roomId);
        
        if (tickables != null) {
            tickables.removeIf(t -> {
                if (t.getId() == tickableId) {
                    Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);
                    if (room != null) {
                        t.onUnregistered(room);
                    }
                    return true;
                }
                return false;
            });
            
            if (tickables.isEmpty()) {
                roomTickables.remove(roomId);
            }
        }
    }
    
    /**
     * Unregisters all tickables for a room.
     * <p>
     * Should be called when a room is unloaded.
     * </p>
     * 
     * @param room the room
     */
    public void unregisterRoom(Room room) {
        if (room == null) {
            return;
        }
        
        Set<WiredTickable> tickables = roomTickables.remove(room.getId());
        
        if (tickables != null) {
            for (WiredTickable tickable : tickables) {
                tickable.onUnregistered(room);
            }
            LOGGER.debug("Unregistered {} tickables from room {}", tickables.size(), room.getId());
        }
    }
    
    /**
     * Resets all timers in a room.
     * 
     * @param room the room
     */
    public void resetRoomTimers(Room room) {
        if (room == null) {
            return;
        }
        
        Set<WiredTickable> tickables = roomTickables.get(room.getId());
        
        if (tickables != null) {
            for (WiredTickable tickable : tickables) {
                try {
                    tickable.resetTimer();
                } catch (Exception e) {
                    LOGGER.error("Error resetting timer for tickable {} in room {}", 
                        tickable.getId(), room.getId(), e);
                }
            }
        }
    }
    
    /**
     * Gets the count of registered tickables for a room.
     * 
     * @param roomId the room ID
     * @return the count
     */
    public int getTickableCount(int roomId) {
        Set<WiredTickable> tickables = roomTickables.get(roomId);
        return tickables != null ? tickables.size() : 0;
    }
    
    /**
     * Gets the total count of registered tickables across all rooms.
     * 
     * @return the total count
     */
    public int getTotalTickableCount() {
        return roomTickables.values().stream()
            .mapToInt(Set::size)
            .sum();
    }
    
    /**
     * Gets the count of rooms with registered tickables.
     * 
     * @return the room count
     */
    public int getActiveRoomCount() {
        return roomTickables.size();
    }
    
    /**
     * The main tick loop.
     * <p>
     * Called at the configured interval by the scheduler. Processes all registered tickables
     * across all rooms.
     * </p>
     */
    private void tick() {
        if (!running.get() || Emulator.isShuttingDown) {
            return;
        }
        
        // Increment global tick counter
        tickCount++;
        
        long startTime = System.currentTimeMillis();
        int tickablesProcessed = 0;
        
        for (Map.Entry<Integer, Set<WiredTickable>> entry : roomTickables.entrySet()) {
            int roomId = entry.getKey();
            Set<WiredTickable> tickables = entry.getValue();
            
            if (tickables.isEmpty()) {
                continue;
            }
            
            // Get the room - skip if not loaded
            Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);
            if (room == null || !room.isLoaded()) {
                continue;
            }
            
            // Skip if room is empty (optimization)
            if (room.getCurrentHabbos().isEmpty() && room.getCurrentBots().isEmpty()) {
                continue;
            }
            
            // Process each tickable
            for (WiredTickable tickable : tickables) {
                try {
                    // Verify item still belongs to this room
                    if (tickable.getRoomId() != roomId) {
                        // Item moved to another room, unregister it
                        tickables.remove(tickable);
                        continue;
                    }
                    
                    // Pass global tick count - all tickables see the same counter
                    // This keeps repeaters with the same interval perfectly synchronized
                    tickable.onWiredTick(room, tickCount, tickIntervalMs);
                    tickablesProcessed++;
                } catch (Exception e) {
                    LOGGER.error("Error in wired tick for tickable {} in room {}: {}", 
                        tickable.getId(), roomId, e.getMessage(), e);
                }
            }
        }
        
        // Debug logging if enabled
        if (debugEnabled && tickablesProcessed > 0) {
            LOGGER.debug("Wired tick #{} completed: {} tickables processed in {}ms", 
                tickCount, tickablesProcessed, System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Gets the current global tick count.
     * 
     * @return the tick count
     */
    public long getTickCount() {
        return tickCount;
    }
}
