package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.CommandLog;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.permissions.PermissionSetting;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetCommand;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.pets.RideablePet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomRightLevels;
import com.eu.habbo.habbohotel.rooms.RoomState;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTypingComposer;
import com.eu.habbo.plugin.events.users.UserCommandEvent;
import com.eu.habbo.plugin.events.users.UserExecuteCommandEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);

    private static final Map<String, Command> commands = new HashMap<>(5);
    private static final Comparator<Command> ALPHABETICAL_ORDER = new Comparator<Command>() {
        public int compare(Command c1, Command c2) {
            String first = c1.permission == null ? c1.keys[0] : c1.permission;
            String second = c2.permission == null ? c2.keys[0] : c2.permission;
            int res = String.CASE_INSENSITIVE_ORDER.compare(first, second);
            return (res != 0) ? res : first.compareTo(second);
        }
    };

    public CommandHandler() {
        long millis = System.currentTimeMillis();
        this.reloadCommands();
        LOGGER.info("Command Handler -> Loaded! ({} MS)", System.currentTimeMillis() - millis);
    }

    public static void addCommand(Command command) {
        if (command == null) return;

        String firstKey = command.keys.length == 0 || command.keys[0] == null ? "" : command.keys[0];
        String registrationKey = command.getClass().getName() + "#" + command.permission + "#" + firstKey;
        commands.put(registrationKey, command);
    }

    public static void addCommand(Class<? extends Command> command) {
        try {
            // command.getConstructor().setAccessible(true);
            addCommand(command.getDeclaredConstructor().newInstance());
            LOGGER.debug("Added command: {}", command.getName());
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }

    public static boolean handleCommand(GameClient gameClient, String commandLine) {
        if (gameClient != null && commandLine != null) {
            if (commandLine.startsWith(":")) {
                commandLine = commandLine.replaceFirst(":", "");

                String[] parts = commandLine.split(" ");

                if (parts.length >= 1) {
                    for (Command command : commands.values()) {
                        for (String s : command.keys) {
                            if (s.equalsIgnoreCase(parts[0])) {
                                boolean succes = false;
                                if (command.permission == null
                                        || gameClient
                                                .getHabbo()
                                                .hasPermission(
                                                        command.permission,
                                                        gameClient
                                                                                        .getHabbo()
                                                                                        .getHabboInfo()
                                                                                        .getCurrentRoom()
                                                                                != null
                                                                        && (gameClient
                                                                                .getHabbo()
                                                                                .getHabboInfo()
                                                                                .getCurrentRoom()
                                                                                .hasRights(gameClient.getHabbo()))
                                                                || gameClient
                                                                        .getHabbo()
                                                                        .hasPermission(Permission.ACC_PLACEFURNI)
                                                                || (gameClient
                                                                                        .getHabbo()
                                                                                        .getHabboInfo()
                                                                                        .getCurrentRoom()
                                                                                != null
                                                                        && gameClient
                                                                                        .getHabbo()
                                                                                        .getHabboInfo()
                                                                                        .getCurrentRoom()
                                                                                        .getGuildId()
                                                                                > 0
                                                                        && gameClient
                                                                                .getHabbo()
                                                                                .getHabboInfo()
                                                                                .getCurrentRoom()
                                                                                .getGuildRightLevel(
                                                                                        gameClient.getHabbo())
                                                                                .isEqualOrGreaterThan(
                                                                                        RoomRightLevels
                                                                                                .GUILD_RIGHTS)))) {
                                    try {
                                        UserExecuteCommandEvent userExecuteCommandEvent =
                                                new UserExecuteCommandEvent(gameClient.getHabbo(), command, parts);
                                        Emulator.getPluginManager().fireEvent(userExecuteCommandEvent);

                                        if (userExecuteCommandEvent.isCancelled()) {
                                            return userExecuteCommandEvent.isSuccess();
                                        }

                                        if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null)
                                            gameClient
                                                    .getHabbo()
                                                    .getHabboInfo()
                                                    .getCurrentRoom()
                                                    .sendComposer(new RoomUserTypingComposer(
                                                                    gameClient
                                                                            .getHabbo()
                                                                            .getRoomUnit(),
                                                                    false)
                                                            .compose());

                                        UserCommandEvent event = new UserCommandEvent(
                                                gameClient.getHabbo(), parts, command.handle(gameClient, parts));
                                        Emulator.getPluginManager().fireEvent(event);

                                        succes = event.succes;
                                    } catch (Exception e) {
                                        LOGGER.error("Caught exception", e);
                                    }

                                    if (gameClient
                                            .getHabbo()
                                            .getHabboInfo()
                                            .getRank()
                                            .isLogCommands()) {
                                        Emulator.getDatabaseLogger()
                                                .store(new CommandLog(
                                                        gameClient
                                                                .getHabbo()
                                                                .getHabboInfo()
                                                                .getId(),
                                                        command,
                                                        commandLine,
                                                        succes));
                                    }
                                }

                                return succes;
                            }
                        }
                    }
                }
            } else {
                String[] args = commandLine.split(" ");

                if (args.length <= 1) return false;

                if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
                    Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();

                    if (room.getCurrentPets().isEmpty()) return false;

                    String normalizedLine = commandLine.trim();
                    for (Pet pet : room.getCurrentPets().values()) {
                        if (pet != null) {
                            String petName = pet.getName() == null ? "" : pet.getName().trim();
                            if (!petName.isEmpty()
                                    && normalizedLine.length() > petName.length()
                                    && normalizedLine.regionMatches(true, 0, petName, 0, petName.length())
                                    && Character.isWhitespace(normalizedLine.charAt(petName.length()))) {
                                String commandText = normalizedLine.substring(petName.length()).trim();

                                for (PetCommand command : pet.getPetData().getPetCommands()) {
                                    if (command != null && command.matches(commandText)) {
                                        if (pet instanceof RideablePet && ((RideablePet) pet).getRider() != null) {
                                            if (((RideablePet) pet)
                                                            .getRider()
                                                            .getHabboInfo()
                                                            .getId()
                                                    == gameClient
                                                            .getHabbo()
                                                            .getHabboInfo()
                                                            .getId()) {
                                                ((RideablePet) pet)
                                                        .getRider()
                                                        .getHabboInfo()
                                                        .dismountPet();
                                            }
                                            break;
                                        }

                                        pet.handleCommand(command, gameClient.getHabbo(), commandText.split("\\s+"));

                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Command getCommand(String key) {
        for (Command command : commands.values()) {
            for (String k : command.keys) {
                if (key.equalsIgnoreCase(k)) {
                    return command;
                }
            }
        }

        return null;
    }

    public void reloadCommands() {
        addCommand(new AboutCommand());
        addCommand(new InvseeCommand());
        addCommand(new AfkCommand());
        addCommand(new AlertCommand());
        addCommand(new AllowTradingCommand());
        addCommand(new ArcturusCommand());
        addCommand(new BadgeCommand());
        addCommand(new BanCommand());
        addCommand(new BlockAlertCommand());
        addCommand(new BotsCommand());
        addCommand(new CalendarCommand());
        addCommand(new ChatTypeCommand());
        addCommand(new CommandsCommand());
        addCommand(new ControlCommand());
        addCommand(new CoordsCommand());
        addCommand(new CreditsCommand());
        addCommand(new DanceCommand());
        addCommand(new DiagonalCommand());
        addCommand(new DisableMassMentionsCommand());
        addCommand(new DisableMentionsCommand());
        addCommand(new DisconnectCommand());
        addCommand(new EjectAllCommand());
        addCommand(new EmptyInventoryCommand());
        addCommand(new EmptyBotsInventoryCommand());
        addCommand(new EmptyPetsInventoryCommand());
        addCommand(new EmuStatsCommand());
        addCommand(new EnableCommand());
        addCommand(new EventCommand());
        addCommand(new FacelessCommand());
        addCommand(new FastwalkCommand());
        addCommand(new FilterWordCommand());
        addCommand(new FreezeBotsCommand());
        addCommand(new FreezeCommand());
        addCommand(new FurniDataCommand());
        addCommand(new FurniValidateCommand());
        addCommand(new LooksValidateCommand());
        addCommand(new GiftCommand());
        addCommand(new GiveRankCommand());
        addCommand(new GiveRespectPointsCommand(
                Emulator.getTexts(),
                Emulator.getConfig(),
                Emulator.getGameEnvironment().getHabboManager()));
        addCommand(new HabnamCommand());
        addCommand(new HandItemCommand());
        addCommand(new HappyHourCommand());
        addCommand(new HideWiredCommand());
        addCommand(new HotelAlertCommand());
        addCommand(new HotelAlertLinkCommand());
        addCommand(new InvisibleCommand());
        addCommand(new ClickInvisibleTilesCommand());
        addCommand(new DebugViewCollisionsCommand());
        addCommand(new IPBanCommand());
        addCommand(new LayCommand());
        addCommand(new MachineBanCommand());
        addCommand(new MassBadgeCommand());
        addCommand(new RoomBadgeCommand());
        addCommand(new MassCreditsCommand());
        addCommand(new MassGiftCommand());
        addCommand(new MassPixelsCommand());
        addCommand(new MassPointsCommand());
        addCommand(new MimicCommand());
        addCommand(new MoonwalkCommand());
        addCommand(new MultiCommand());
        addCommand(new MuteBotsCommand());
        addCommand(new MuteCommand());
        addCommand(new MutePetsCommand());
        addCommand(new MaxPetStatCommand());
        addCommand(new AutoStackHeightCommand());
        addCommand(new HotelNotificationCommand());
        addCommand(new PokerNotificationCommand());
        addCommand(RoomDoorbellCommand.close());
        addCommand(RoomDoorbellCommand.open());
        addCommand(new RideHorseCommand());
        addCommand(new OnlineCommand());
        addCommand(new PetInfoCommand());
        addCommand(new PickallCommand());
        addCommand(new PingCommand());
        addCommand(new PixelCommand());
        addCommand(new PluginsCommand());
        addCommand(new PointsCommand());
        addCommand(new KissCommand());
        addCommand(new PunchCommand());
        addCommand(new BssPreferenceCommand("cmd_bss_dnd", BssCommandPreferences.Flag.DO_NOT_DISTURB));
        addCommand(new BssPreferenceCommand("cmd_bss_block_gifts", BssCommandPreferences.Flag.BLOCK_GIFTS));
        addCommand(new BssPreferenceCommand("cmd_bss_block_whispers", BssCommandPreferences.Flag.BLOCK_WHISPERS));
        addCommand(new BssPreferenceCommand("cmd_bss_block_mimic", BssCommandPreferences.Flag.BLOCK_MIMIC));
        addCommand(new BssPreferenceCommand("cmd_bss_block_kisses", BssCommandPreferences.Flag.BLOCK_KISSES));
        addCommand(new BssPreferenceCommand("cmd_bss_group_chat", BssCommandPreferences.Flag.GROUP_CHAT_ENABLED));
        addCommand(new BssPreferenceCommand("cmd_bss_user_click", BssCommandPreferences.Flag.USER_CLICK_ENABLED));
        addCommand(new BssPreferenceCommand("cmd_bss_random_walk", BssCommandPreferences.Flag.RANDOM_WALK_PRIORITY));
        addCommand(new BssKickPetsCommand());
        addCommand(new BssKickBotsCommand());
        addCommand(new BssRegenerateMapsCommand());
        addCommand(new BssCloseDiceCommand());
        addCommand(new BanzaiSpeedCommand());
        addCommand(new BssRoomStateCommand("cmd_bss_open_room", RoomState.OPEN));
        addCommand(new BssRoomStateCommand("cmd_bss_close_room", RoomState.LOCKED));
        addCommand(new BssDisableEffectCommand());
        addCommand(new BssReloadCreditsCommand());
        addCommand(new BssRoomBanCommand());
        addCommand(new BssTogglePyramidsCommand());
        addCommand(new BssNotificationCommand());
        addCommand(new BssOpenPokerCommand());
        addCommand(new BssTagCommand("cmd_bss_add_tag", BssTagCommand.Operation.ADD));
        addCommand(new BssTagCommand("cmd_bss_remove_tag", BssTagCommand.Operation.REMOVE));
        addCommand(new BssTagCommand("cmd_bss_clear_tags", BssTagCommand.Operation.CLEAR));
        addCommand(new BssPersonalTradeCommand());
        addCommand(new BssResetPrefixCommand());
        addCommand(new BssReportCommand());
        addCommand(new BssClearGroupChatCommand());
        addCommand(new BssRedeemCurrencyCommand("cmd_bss_convert_credits", BssRedeemCurrencyCommand.Mode.CREDITS));
        addCommand(new BssRedeemCurrencyCommand("cmd_bss_convert_diamonds", BssRedeemCurrencyCommand.Mode.DIAMONDS));
        addCommand(new BssRareValueCommand("cmd_bss_rare_value", BssRareValueCommand.Scope.ITEM));
        addCommand(new BssRareValueCommand("cmd_bss_inventory_value", BssRareValueCommand.Scope.INVENTORY));
        addCommand(new BssRareValueCommand("cmd_bss_room_value", BssRareValueCommand.Scope.ROOM));
        addCommand(new BssGivePrizeCommand());
        addCommand(new BssPlacementCommand("cmd_bss_placex", BssPlacementCommand.Mode.BATCH));
        addCommand(new BssPlacementCommand("cmd_bss_force_height", BssPlacementCommand.Mode.FORCE_HEIGHT));
        addCommand(new BssPlacementCommand("cmd_bss_force_rotation", BssPlacementCommand.Mode.FORCE_ROTATION));
        addCommand(new PromoteTargetOfferCommand());
        addCommand(new PullCommand());
        addCommand(new PushCommand());
        addCommand(new TogglePullPushCommand(true));
        addCommand(new TogglePullPushCommand(false));
        addCommand(new ToggleTradeCommand());
        addCommand(new RedeemCommand());
        addCommand(new ReloadRoomCommand());
        addCommand(new RoomAlertCommand());
        addCommand(new RoomBundleCommand());
        addCommand(new RoomCreditsCommand());
        addCommand(new RoomDanceCommand());
        addCommand(new RoomEffectCommand());
        addCommand(new RoomItemCommand());
        addCommand(new RoomKickCommand());
        addCommand(new RoomMuteCommand());
        addCommand(new RoomPixelsCommand());
        addCommand(new RoomPointsCommand());
        addCommand(new SayAllCommand());
        addCommand(new SayCommand());
        addCommand(new SetMaxCommand());
        addCommand(new SetHomeCommand());
        addCommand(new SetPollCommand());
        addCommand(new SetRoomTemplateCommand());
        addCommand(new SetSpeedCommand());
        addCommand(new ShoutAllCommand());
        addCommand(new ShoutCommand());
        addCommand(new ShutdownCommand());
        addCommand(new SitCommand());
        addCommand(new SnowWarSaveCommand());
        addCommand(new StandCommand());
        addCommand(new SitDownCommand());
        addCommand(new StaffAlertCommand());
        addCommand(new StaffOnlineCommand());
        addCommand(new StalkCommand());
        addCommand(new SummonCommand());
        addCommand(new SummonRankCommand());
        addCommand(new SuperbanCommand());
        addCommand(new SuperPullCommand());
        addCommand(new TakeBadgeCommand());
        addCommand(new TeleportCommand());
        addCommand(new TransformCommand());
        addCommand(new TrashCommand());
        addCommand(new FunRoomCommand());
        addCommand(new UnbanCommand());
        addCommand(new UnloadRoomCommand());
        addCommand(new UnmuteCommand());
        addCommand(new UpdateAllCommand());
        addCommand(new UpdateAchievements());
        addCommand(new UpdateBotsCommand());
        addCommand(new UpdateCalendarCommand());
        addCommand(new UpdateCatalogCommand());
        addCommand(new UpdateConfigCommand());
        addCommand(new UpdateGuildPartsCommand());
        addCommand(new UpdateHotelViewCommand());
        addCommand(new UpdateItemsCommand());
        addCommand(new UpdateNavigatorCommand());
        addCommand(new UpdatePermissionsCommand());
        addCommand(new UpdatePetDataCommand());
        addCommand(new UpdatePluginsCommand());
        addCommand(new UpdatePollsCommand());
        addCommand(new UpdateTextsCommand());
        addCommand(new UpdateWordFilterCommand());
        addCommand(new UserInfoCommand());
        addCommand(new WordQuizCommand());
        addCommand(new UpdateYoutubePlaylistsCommand());
        addCommand(new AddYoutubePlaylistCommand());
        addCommand(new SoftKickCommand());
        addCommand(new SubscriptionCommand());
        addCommand(new UpdateChatBubblesCommand());
        addCommand(new GivePrefixCommand());
        addCommand(new ListPrefixesCommand());
        addCommand(new RemovePrefixCommand());
        addCommand(new WiredCommand());
        addCommand(new WiredHelpCommand());
        addCommand(new TestCommand());
    }

    public List<Command> getCommandsForRank(int rankId) {
        List<Command> allowedCommands = new ArrayList<>();
        if (Emulator.getGameEnvironment().getPermissionsManager().rankExists(rankId)) {
            Map<String, Permission> permissions = Emulator.getGameEnvironment()
                    .getPermissionsManager()
                    .getRank(rankId)
                    .getPermissions();

            for (Command command : commands.values()) {
                if (allowedCommands.contains(command)) continue;

                if (command.permission == null
                        || (permissions.containsKey(command.permission)
                        && permissions.get(command.permission).setting != PermissionSetting.DISALLOWED)) {
                    allowedCommands.add(command);
                }
            }
        }

        allowedCommands.sort(CommandHandler.ALPHABETICAL_ORDER);

        return allowedCommands;
    }

    public void dispose() {
        commands.clear();
        LOGGER.info("Command Handler -> Disposed!");
    }
}
