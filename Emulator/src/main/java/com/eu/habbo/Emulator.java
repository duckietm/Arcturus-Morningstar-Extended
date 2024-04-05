package com.eu.habbo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.eu.habbo.core.*;
import com.eu.habbo.core.consolecommands.ConsoleCommand;
import com.eu.habbo.database.Database;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.networking.camera.CameraClient;
import com.eu.habbo.networking.gameserver.GameServer;
import com.eu.habbo.networking.rconserver.RCONServer;
import com.eu.habbo.plugin.PluginManager;
import com.eu.habbo.plugin.events.emulator.EmulatorConfigUpdatedEvent;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.emulator.EmulatorStartShutdownEvent;
import com.eu.habbo.plugin.events.emulator.EmulatorStoppedEvent;
import com.eu.habbo.threading.ThreadPooling;
import com.eu.habbo.util.imager.badges.BadgeImager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Emulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Emulator.class);
    private static final String OS_NAME = (System.getProperty("os.name") != null ? System.getProperty("os.name") : "Unknown");
    private static final String CLASS_PATH = (System.getProperty("java.class.path") != null ? System.getProperty("java.class.path") : "Unknown");

    public final static int MAJOR = 3;
    public final static int MINOR = 5;
    public final static int BUILD = 1;
    public final static String PREVIEW = "";

    public static final String version = "Arcturus Morningstar" + " " + MAJOR + "." + MINOR + "." + BUILD + " " + PREVIEW;
    private static final String logo =
            "\n" +
                    "███╗   ███╗ ██████╗ ██████╗ ███╗   ██╗██╗███╗   ██╗ ██████╗ ███████╗████████╗ █████╗ ██████╗ \n" +
                    "████╗ ████║██╔═══██╗██╔══██╗████╗  ██║██║████╗  ██║██╔════╝ ██╔════╝╚══██╔══╝██╔══██╗██╔══██╗\n" +
                    "██╔████╔██║██║   ██║██████╔╝██╔██╗ ██║██║██╔██╗ ██║██║  ███╗███████╗   ██║   ███████║██████╔╝\n" +
                    "██║╚██╔╝██║██║   ██║██╔══██╗██║╚██╗██║██║██║╚██╗██║██║   ██║╚════██║   ██║   ██╔══██║██╔══██╗\n" +
                    "██║ ╚═╝ ██║╚██████╔╝██║  ██║██║ ╚████║██║██║ ╚████║╚██████╔╝███████║   ██║   ██║  ██║██║  ██║\n" +
                    "╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝╚═╝  ╚═══╝ ╚═════╝ ╚══════╝   ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝\n" ;


    public static String build = "";
    public static boolean isReady = false;
    public static boolean isShuttingDown = false;
    public static boolean stopped = false;
    public static boolean debugging = false;
    private static int timeStarted = 0;
    private static Runtime runtime;
    private static ConfigurationManager config;
    private static CryptoConfig crypto;
    private static TextsManager texts;
    private static GameServer gameServer;
    private static RCONServer rconServer;
    private static CameraClient cameraClient;
    private static Logging logging;
    private static Database database;
    private static DatabaseLogger databaseLogger;
    private static ThreadPooling threading;
    private static GameEnvironment gameEnvironment;
    private static PluginManager pluginManager;
    private static BadgeImager badgeImager;

    static {
        Thread hook = new Thread(new Runnable() {
            public synchronized void run() {
                Emulator.dispose();
            }
        });
        hook.setPriority(10);
        Runtime.getRuntime().addShutdownHook(hook);
    }

    public static void promptEnterKey(){
        System.out.println("\n");
        System.out.println("Press \"ENTER\" if you agree to the terms stated above...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public static void main(String[] args) throws Exception {
        try {
            // Check if running on Windows and not in IntelliJ.
            // If so, we need to reconfigure the console appender and enable Jansi for colors.
            if (OS_NAME.startsWith("Windows") && !CLASS_PATH.contains("idea_rt.jar")) {
                ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) root.getAppender("Console");

                appender.stop();
                appender.setWithJansi(true);
                appender.start();
            }

            Locale.setDefault(new Locale("en"));
            setBuild();
            Emulator.stopped = false;
            ConsoleCommand.load();
            Emulator.logging = new Logging();

            System.out.println(logo);

            // Checks if this is a BETA build before allowing them to continue.
            if (PREVIEW.toLowerCase().contains("beta")) {
                System.out.println("Warning, this is a beta build, this means that there may be unintended consequences so make sure you take regular backups while using this build. If you notice any issues you should make an issue on the Krews Git.");
                promptEnterKey();
            }
            LOGGER.info("eek. Has it really been a year?");
            LOGGER.info("This project is for educational purposes only. This Emulator is an open-source fork of Arcturus created by TheGeneral.");
            LOGGER.info("Version: {}", version);
            LOGGER.info("Build: {}", build);
            LOGGER.info("Follow our development at https://git.krews.org/morningstar/Arcturus-Community");

            long startTime = System.nanoTime();

            Emulator.runtime = Runtime.getRuntime();
            Emulator.config = new ConfigurationManager("config.ini");
            Emulator.crypto = new CryptoConfig(
                    Emulator.getConfig().getBoolean("enc.enabled", false),
                    Emulator.getConfig().getValue("enc.e"),
                    Emulator.getConfig().getValue("enc.n"),
                    Emulator.getConfig().getValue("enc.d"));
            Emulator.database = new Database(Emulator.getConfig());
            Emulator.databaseLogger = new DatabaseLogger();
            Emulator.config.loaded = true;
            Emulator.config.loadFromDatabase();
            Emulator.threading = new ThreadPooling(Emulator.getConfig().getInt("runtime.threads"));
            Emulator.getDatabase().getDataSource().setMaximumPoolSize(Emulator.getConfig().getInt("runtime.threads") * 2);
            Emulator.getDatabase().getDataSource().setMinimumIdle(10);
            Emulator.pluginManager = new PluginManager();
            Emulator.pluginManager.reload();
            Emulator.getPluginManager().fireEvent(new EmulatorConfigUpdatedEvent());
            Emulator.texts = new TextsManager();
            new CleanerThread();
            Emulator.gameServer = new GameServer(getConfig().getValue("game.host", "127.0.0.1"), getConfig().getInt("game.port", 30000));
            Emulator.rconServer = new RCONServer(getConfig().getValue("rcon.host", "127.0.0.1"), getConfig().getInt("rcon.port", 30001));
            Emulator.gameEnvironment = new GameEnvironment();
            Emulator.gameEnvironment.load();
            Emulator.gameServer.initializePipeline();
            Emulator.gameServer.connect();
            Emulator.rconServer.initializePipeline();
            Emulator.rconServer.connect();
            Emulator.badgeImager = new BadgeImager();

            LOGGER.info("Arcturus Morningstar has successfully loaded.");
            LOGGER.info("System launched in: {}ms. Using {} threads!", (System.nanoTime() - startTime) / 1e6, Runtime.getRuntime().availableProcessors() * 2);
            LOGGER.info("Memory: {}/{}MB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024), (runtime.freeMemory()) / (1024 * 1024));

            Emulator.debugging = Emulator.getConfig().getBoolean("debug.mode");

            if (debugging) {
                ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                root.setLevel(Level.DEBUG);
                LOGGER.debug("Debugging enabled.");
            }

            Emulator.getPluginManager().fireEvent(new EmulatorLoadedEvent());
            Emulator.isReady = true;
            Emulator.timeStarted = getIntUnixTimestamp();

            if (Emulator.getConfig().getInt("runtime.threads") < (Runtime.getRuntime().availableProcessors() * 2)) {
                LOGGER.warn("Emulator settings runtime.threads ({}) can be increased to ({}) to possibly increase performance.",
                        Emulator.getConfig().getInt("runtime.threads"),
                        Runtime.getRuntime().availableProcessors() * 2);
            }

            Emulator.getThreading().run(() -> {
            }, 1500);

            // Check if console mode is true or false, default is true
            if (Emulator.getConfig().getBoolean("console.mode", true)) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                while (!isShuttingDown && isReady) {
                    try {
                        String line = reader.readLine();

                        if (line != null) {
                            ConsoleCommand.handle(line);
                        }
                        System.out.println("Waiting for command: ");
                    } catch (Exception e) {
                        if (!(e instanceof IOException && e.getMessage().equals("Bad file descriptor"))) {
                            LOGGER.error("Error while reading command", e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setBuild() {
        if (Emulator.class.getProtectionDomain().getCodeSource() == null) {
            build = "UNKNOWN";
            return;
        }

        StringBuilder sb = new StringBuilder();
        try {
            String filepath = new File(Emulator.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
            MessageDigest md = MessageDigest.getInstance("MD5");// MD5
            FileInputStream fis = new FileInputStream(filepath);
            byte[] dataBytes = new byte[1024];
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1)
                md.update(dataBytes, 0, nread);
            byte[] mdbytes = md.digest();
            for (int i = 0; i < mdbytes.length; i++)
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        } catch (Exception e) {
            build = "UNKNOWN";
            return;
        }

        build = sb.toString();
    }

    private static void dispose() {
        Emulator.getThreading().setCanAdd(false);
        Emulator.isShuttingDown = true;
        Emulator.isReady = false;

        LOGGER.info("Stopping Arcturus Morningstar {}", version);

        try {
            if (Emulator.getPluginManager() != null)
                Emulator.getPluginManager().fireEvent(new EmulatorStartShutdownEvent());
        } catch (Exception e) {
        }

        try {
            if (Emulator.cameraClient != null)
                Emulator.cameraClient.disconnect();
        } catch (Exception e) {
        }

        try {
            if (Emulator.rconServer != null)
                Emulator.rconServer.stop();
        } catch (Exception e) {
        }

        try {
            if (Emulator.gameEnvironment != null)
                Emulator.gameEnvironment.dispose();
        } catch (Exception e) {
        }

        try {
            if (Emulator.getPluginManager() != null)
                Emulator.getPluginManager().fireEvent(new EmulatorStoppedEvent());
        } catch (Exception e) {
        }

        try {
            if (Emulator.pluginManager != null)
                Emulator.pluginManager.dispose();
        } catch (Exception e) {
        }

        try {
            if (Emulator.config != null) {
                Emulator.config.saveToDatabase();
            }
        } catch (Exception e) {
        }

        try {
            if (Emulator.gameServer != null)
                Emulator.gameServer.stop();
        } catch (Exception e) {
        }

        LOGGER.info("Stopped Arcturus Morningstar {}", version);

        if (Emulator.database != null) {
            Emulator.getDatabase().dispose();
        }
        Emulator.stopped = true;

        // if (osName.startsWith("Windows") && (!classPath.contains("idea_rt.jar"))) {
        //     AnsiConsole.systemUninstall();
        // }
        try {
            if (Emulator.threading != null)

                Emulator.threading.shutDown();
        } catch (Exception e) {
        }
    }

    public static ConfigurationManager getConfig() {
        return config;
    }

    public static CryptoConfig getCrypto() {
        return crypto;
    }

    public static TextsManager getTexts() {
        return texts;
    }

    public static Database getDatabase() {
        return database;
    }

    public static DatabaseLogger getDatabaseLogger() {
        return databaseLogger;
    }

    public static Runtime getRuntime() {
        return runtime;
    }

    public static GameServer getGameServer() {
        return gameServer;
    }

    public static RCONServer getRconServer() {
        return rconServer;
    }

    /**
     * @deprecated Do not use. Please use LoggerFactory.getLogger(YourClass.class) to log.
     */
    @Deprecated
    public static Logging getLogging() {
        return logging;
    }

    public static ThreadPooling getThreading() {
        return threading;
    }

    public static GameEnvironment getGameEnvironment() {
        return gameEnvironment;
    }

    public static PluginManager getPluginManager() {
        return pluginManager;
    }

    public static Random getRandom() {
        return ThreadLocalRandom.current();
    }

    public static BadgeImager getBadgeImager() {
        return badgeImager;
    }

    public static CameraClient getCameraClient() {
        return cameraClient;
    }

    public static synchronized void setCameraClient(CameraClient client) {
        cameraClient = client;
    }

    public static int getTimeStarted() {
        return timeStarted;
    }

    public static int getOnlineTime() {
        return getIntUnixTimestamp() - timeStarted;
    }

    public static void prepareShutdown() {
        System.exit(0);
    }

    public static int timeStringToSeconds(String timeString) {
        int totalSeconds = 0;

        Matcher m = Pattern.compile("(([0-9]*) (second|minute|hour|day|week|month|year))").matcher(timeString);
        Map<String,Integer> map = new HashMap<String,Integer>() {
            {
                put("second", 1);
                put("minute", 60);
                put("hour", 3600);
                put("day", 86400);
                put("week", 604800);
                put("month", 2628000);
                put("year", 31536000);
            }
        };

        while (m.find()) {
            try {
                int amount = Integer.parseInt(m.group(2));
                String what = m.group(3);
                totalSeconds += amount * map.get(what);
            }
            catch (Exception ignored) { }
        }

        return totalSeconds;
    }

    public static Date modifyDate(Date date, String timeString) {
        int totalSeconds = 0;

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        Matcher m = Pattern.compile("(([0-9]*) (second|minute|hour|day|week|month|year))").matcher(timeString);
        Map<String, Integer> map = new HashMap<String, Integer>() {
            {
                put("second", Calendar.SECOND);
                put("minute", Calendar.MINUTE);
                put("hour", Calendar.HOUR);
                put("day", Calendar.DAY_OF_MONTH);
                put("week", Calendar.WEEK_OF_MONTH);
                put("month", Calendar.MONTH);
                put("year", Calendar.YEAR);
            }
        };

        while (m.find()) {
            try {
                int amount = Integer.parseInt(m.group(2));
                String what = m.group(3);
                c.add(map.get(what), amount);
            }
            catch (Exception ignored) { }
        }

        return c.getTime();
    }

    private static String dateToUnixTimestamp(Date date) {
        String res = "";
        Date aux = stringToDate("1970-01-01 00:00:00");
        Timestamp aux1 = dateToTimeStamp(aux);
        Timestamp aux2 = dateToTimeStamp(date);
        long difference = aux2.getTime() - aux1.getTime();
        long seconds = difference / 1000L;
        return res + seconds;
    }

    public static Date stringToDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date res = null;
        try {
            res = format.parse(date);
        } catch (Exception e) {
            LOGGER.error("Error parsing date", e);
        }
        return res;
    }

    public static Timestamp dateToTimeStamp(Date date) {
        return new Timestamp(date.getTime());
    }

    public static Date getDate() {
        return new Date(System.currentTimeMillis());
    }

    public static String getUnixTimestamp() {
        return dateToUnixTimestamp(getDate());
    }

    public static int getIntUnixTimestamp() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static boolean isNumeric(String string)
            throws IllegalArgumentException {
        boolean isnumeric = false;
        if ((string != null) && (!string.equals(""))) {
            isnumeric = true;
            char[] chars = string.toCharArray();
            for (char aChar : chars) {
                isnumeric = Character.isDigit(aChar);
                if (!isnumeric) {
                    break;
                }
            }
        }
        return isnumeric;
    }

    public int getUserCount() {
        return gameEnvironment.getHabboManager().getOnlineCount();
    }

    public int getRoomCount() {
        return gameEnvironment.getRoomManager().getActiveRooms().size();
    }
}
