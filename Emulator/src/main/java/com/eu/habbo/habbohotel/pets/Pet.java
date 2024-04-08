package com.eu.habbo.habbohotel.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.rooms.*;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ISerialize;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.rooms.pets.PetLevelUpdatedComposer;
import com.eu.habbo.messages.outgoing.rooms.pets.RoomPetExperienceComposer;
import com.eu.habbo.messages.outgoing.rooms.pets.RoomPetRespectComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserRemoveComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;
import com.eu.habbo.plugin.events.pets.PetTalkEvent;
import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

public class Pet implements ISerialize, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Pet.class);

    public int levelThirst;
    public int levelHunger;
    public boolean needsUpdate = false;
    public boolean packetUpdate = false;
    protected int id;
    protected int userId;
    protected Room room;
    protected String name;
    protected PetData petData;
    protected int race;
    protected String color;
    protected int happyness;
    protected int experience;
    protected int energy;
    protected int respect;
    protected int created;
    protected int level;
    RoomUnit roomUnit;
    private int chatTimeout;
    private int tickTimeout = Emulator.getIntUnixTimestamp();
    private int happynessDelay = Emulator.getIntUnixTimestamp();
    private int gestureTickTimeout = Emulator.getIntUnixTimestamp();
    private int randomActionTickTimeout = Emulator.getIntUnixTimestamp();
    private int postureTimeout = Emulator.getIntUnixTimestamp();
    private int stayStartedAt = 0;
    private int idleCommandTicks = 0;
    private int freeCommandTicks = -1;

    private PetTasks task = PetTasks.FREE;

    private boolean muted = false;

    public Pet(ResultSet set) throws SQLException {
        super();
        this.id = set.getInt("id");
        this.userId = set.getInt("user_id");
        this.room = null;
        this.name = set.getString("name");
        this.petData = Emulator.getGameEnvironment().getPetManager().getPetData(set.getInt("type"));
        if (this.petData == null) {
            LOGGER.error("WARNING! Missing pet data for type: " + set.getInt("type") + "! Insert a new entry into the pet_actions table for this type!");
            this.petData = Emulator.getGameEnvironment().getPetManager().getPetData(0);
        }
        this.race = set.getInt("race");
        this.experience = set.getInt("experience");
        this.happyness = set.getInt("happyness");
        this.energy = set.getInt("energy");
        this.respect = set.getInt("respect");
        this.created = set.getInt("created");
        this.color = set.getString("color");
        this.levelThirst = set.getInt("thirst");
        this.levelHunger = set.getInt("hunger");
        this.level = PetManager.getLevel(this.experience);
    }

    public Pet(int type, int race, String color, String name, int userId) {
        this.id = 0;
        this.userId = userId;
        this.room = null;
        this.name = name;
        this.petData = Emulator.getGameEnvironment().getPetManager().getPetData(type);

        if (this.petData == null) {
            LOGGER.warn("Missing pet data for type: " + type + "! Insert a new entry into the pet_actions table for this type!");
        }

        this.race = race;
        this.color = color;
        this.experience = 0;
        this.happyness = 100;
        this.energy = 100;
        this.respect = 0;
        this.levelThirst = 0;
        this.levelHunger = 0;
        this.created = Emulator.getIntUnixTimestamp();
        this.level = 1;
    }


    protected void say(String message) {
        if (this.roomUnit != null && this.room != null && !message.isEmpty()) {
            RoomChatMessage chatMessage = new RoomChatMessage(message, this.roomUnit, RoomChatMessageBubbles.NORMAL);
            PetTalkEvent talkEvent = new PetTalkEvent(this, chatMessage);
            if (!Emulator.getPluginManager().fireEvent(talkEvent).isCancelled()) {
                this.room.petChat(new RoomUserTalkComposer(chatMessage).compose());
            }
        }
    }


    public void say(PetVocal vocal) {
        if (vocal != null)
            this.say(vocal.message);
    }


    public void addEnergy(int amount) {
        this.energy += amount;

        if (this.energy > PetManager.maxEnergy(this.level))
            this.energy = PetManager.maxEnergy(this.level);

        if (this.energy < 0)
            this.energy = 0;
    }


    public void addHappyness(int amount) {
        this.happyness += amount;

        if (this.happyness > 100)
            this.happyness = 100;

        if (this.happyness < 0)
            this.happyness = 0;
    }

    public int getRespect() {
        return this.respect;
    }

    public void addRespect() {
        this.respect++;
    }


    public int daysAlive() {
        return (Emulator.getIntUnixTimestamp() - this.created) / 86400;
    }


    public String bornDate() {

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTime(new java.util.Date(this.created));

        return cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.YEAR);
    }

    @Override
    public void run() {
        if (this.needsUpdate) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                if (this.id > 0) {
                    try (PreparedStatement statement = connection.prepareStatement("UPDATE users_pets SET room_id = ?, experience = ?, energy = ?, respect = ?, x = ?, y = ?, z = ?, rot = ?, hunger = ?, thirst = ?, happyness = ?, created = ? WHERE id = ?")) {
                        statement.setInt(1, (this.room == null ? 0 : this.room.getId()));
                        statement.setInt(2, this.experience);
                        statement.setInt(3, this.energy);
                        statement.setInt(4, this.respect);
                        statement.setInt(5, this.roomUnit != null ? this.roomUnit.getX() : 0);
                        statement.setInt(6, this.roomUnit != null ? this.roomUnit.getY() : 0);
                        statement.setDouble(7, this.roomUnit != null ? this.roomUnit.getZ() : 0.0);
                        statement.setInt(8, this.roomUnit != null ? this.roomUnit.getBodyRotation().getValue() : 0);
                        statement.setInt(9, this.levelHunger);
                        statement.setInt(10, this.levelThirst);
                        statement.setInt(11, this.happyness);
                        statement.setInt(12, this.created);
                        statement.setInt(13, this.id);
                        statement.execute();
                    }
                } else if (this.id == 0) {
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users_pets (user_id, room_id, name, race, type, color, experience, energy, respect, created) VALUES (?, 0, ?, ?, ?, ?, 0, 0, 0, ?)", Statement.RETURN_GENERATED_KEYS)) {
                        statement.setInt(1, this.userId);
                        statement.setString(2, this.name);
                        statement.setInt(3, this.race);
                        statement.setInt(4, 0);

                        if (this.petData != null) {
                            statement.setInt(4, this.petData.getType());
                        }

                        statement.setString(5, this.color);
                        statement.setInt(6, this.created);
                        statement.execute();

                        try (ResultSet set = statement.getGeneratedKeys()) {
                            if (set.next()) {
                                this.id = set.getInt(1);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }

            this.needsUpdate = false;
        }
    }

    public void cycle() {
        this.idleCommandTicks++;

        int time = Emulator.getIntUnixTimestamp();
        if (this.roomUnit != null && this.task != PetTasks.RIDE) {
            if (time - this.gestureTickTimeout > 5 && this.roomUnit.hasStatus(RoomUnitStatus.GESTURE)) {
                this.roomUnit.removeStatus(RoomUnitStatus.GESTURE);
                this.packetUpdate = true;
            }

            if (time - this.postureTimeout > 1 && this.task == null) {
                this.clearPosture();
                this.postureTimeout = time + 120;
            }

            if (this.freeCommandTicks > 0) {
                this.freeCommandTicks--;

                if (this.freeCommandTicks == 0) {
                    this.freeCommand();
                }
            }

            if (!this.roomUnit.isWalking()) {
                if (this.roomUnit.getWalkTimeOut() < time && this.canWalk()) {
                    RoomTile tile = this.room.getRandomWalkableTile();

                    if (tile != null) {
                        this.roomUnit.setGoalLocation(tile);
                    }
                }

                if (this.task == PetTasks.NEST || this.task == PetTasks.DOWN) {
                    if (this.levelHunger > 0)
                        this.levelHunger--;

                    if (this.levelThirst > 0)
                        this.levelThirst--;

                    this.addEnergy(5);

                    this.addHappyness(1);

                    if (this.energy == PetManager.maxEnergy(this.level)) {
                        this.roomUnit.removeStatus(RoomUnitStatus.LAY);
                        this.roomUnit.setCanWalk(true);
                        this.roomUnit.setGoalLocation(this.room.getRandomWalkableTile());
                        this.task = null;
                        this.roomUnit.setStatus(RoomUnitStatus.GESTURE, PetGestures.ENERGY.getKey());
                        this.gestureTickTimeout = time;
                    }
                } else if (this.tickTimeout >= 5) {
                    if (this.levelHunger < 100)
                        this.levelHunger++;

                    if (this.levelThirst < 100)
                        this.levelThirst++;

                    if (this.energy < PetManager.maxEnergy(this.level))
                        this.energy++;

                    this.tickTimeout = time;
                }

                if (this.task == PetTasks.STAY && Emulator.getIntUnixTimestamp() - this.stayStartedAt >= 120) {
                    this.task = null;
                    this.getRoomUnit().setCanWalk(true);
                }
            } else {
                int timeout = Emulator.getRandom().nextInt(10) * 2;
                this.roomUnit.setWalkTimeOut(timeout < 20 ? 20 + time : timeout + time);

                if (this.energy >= 2)
                    this.addEnergy(-1);

                if (this.levelHunger < 100)
                    this.levelHunger++;

                if (this.levelThirst < 100)
                    this.levelThirst++;

                if (this.happyness > 0 && time - this.happynessDelay >= 30) {
                    this.happyness--;
                    this.happynessDelay = time;
                }
            }

            if (time - this.gestureTickTimeout > 15) {
                this.updateGesture(time);
            } else if (time - this.randomActionTickTimeout > 30) {
                this.randomAction();
                this.randomActionTickTimeout = time + (10 * Emulator.getRandom().nextInt(60));
            }

            if (!this.muted) {
                if (this.chatTimeout <= time) {
                    if (this.energy <= 30) {
                        this.say(this.petData.randomVocal(PetVocalsType.TIRED));
                        if (this.energy <= 10)
                            this.findNest();
                    } else if (this.happyness > 85) {
                        this.say(this.petData.randomVocal(PetVocalsType.GENERIC_HAPPY));
                    } else if (this.happyness < 15) {
                        this.say(this.petData.randomVocal(PetVocalsType.GENERIC_SAD));
                    } else if (this.levelHunger > 50) {
                        this.say(this.petData.randomVocal(PetVocalsType.HUNGRY));
                        this.eat();
                    } else if (this.levelThirst > 50) {
                        this.say(this.petData.randomVocal(PetVocalsType.THIRSTY));
                        this.drink();
                    }

                    int timeOut = Emulator.getRandom().nextInt(30);
                    this.chatTimeout = time + (timeOut < 3 ? 30 : timeOut);
                }
            }
        }
    }


    public void handleCommand(PetCommand command, Habbo habbo, String[] data) {
        this.idleCommandTicks = 0;

        if (this.task == PetTasks.STAY) {
            this.stayStartedAt = 0;
            this.task = null;
            this.getRoomUnit().setCanWalk(true);
        }

        command.handle(this, habbo, data);


    }

    public boolean canWalk() {
        if (this.task == null)
            return true;

        switch (this.task) {
            case DOWN:
            case FLAT:
            case HERE:
            case SIT:
            case BEG:
            case PLAY:
            case PLAY_FOOTBALL:
            case PLAY_DEAD:
            case FOLLOW:
            case JUMP:
            case STAND:
            case NEST:
            case RIDE:

                return false;
        }

        return true;
    }

    public void clearPosture() {
        THashMap<RoomUnitStatus, String> keys = new THashMap<>();

        if (this.roomUnit.hasStatus(RoomUnitStatus.MOVE))
            keys.put(RoomUnitStatus.MOVE, this.roomUnit.getStatus(RoomUnitStatus.MOVE));

        if (this.roomUnit.hasStatus(RoomUnitStatus.SIT))
            keys.put(RoomUnitStatus.SIT, this.roomUnit.getStatus(RoomUnitStatus.SIT));

        if (this.roomUnit.hasStatus(RoomUnitStatus.LAY))
            keys.put(RoomUnitStatus.LAY, this.roomUnit.getStatus(RoomUnitStatus.LAY));

        if (this.roomUnit.hasStatus(RoomUnitStatus.GESTURE))
            keys.put(RoomUnitStatus.GESTURE, this.roomUnit.getStatus(RoomUnitStatus.GESTURE));

        if (this.task == null) {
            boolean isDead = false;
            if (this.roomUnit.hasStatus(RoomUnitStatus.RIP))
                isDead = true;

            this.roomUnit.clearStatus();

            if (isDead) this.roomUnit.setStatus(RoomUnitStatus.RIP, "");
            for (Map.Entry<RoomUnitStatus, String> entry : keys.entrySet()) {
                this.roomUnit.setStatus(entry.getKey(), entry.getValue());
            }

            if (!keys.isEmpty()) this.packetUpdate = true;
        }
    }

    public void updateGesture(int time) {
        this.gestureTickTimeout = time;
        if (this.energy < 30) {
            this.roomUnit.setStatus(RoomUnitStatus.GESTURE, PetGestures.TIRED.getKey());
            this.findNest();
        } else if (this.happyness == 100) {
            this.roomUnit.setStatus(RoomUnitStatus.GESTURE, PetGestures.LOVE.getKey());
        } else if (this.happyness >= 90) {
            this.randomHappyAction();
            this.roomUnit.setStatus(RoomUnitStatus.GESTURE, PetGestures.HAPPY.getKey());
        } else if (this.happyness <= 5) {
            this.randomSadAction();
            this.roomUnit.setStatus(RoomUnitStatus.GESTURE, PetGestures.SAD.getKey());
        } else if (this.levelHunger > 80) {
            this.roomUnit.setStatus(RoomUnitStatus.GESTURE, PetGestures.HUNGRY.getKey());
            this.eat();
        } else if (this.levelThirst > 80) {
            this.roomUnit.setStatus(RoomUnitStatus.GESTURE, PetGestures.THIRSTY.getKey());
            this.drink();
        } else if (this.idleCommandTicks > 240) {
            this.idleCommandTicks = 0;

            this.roomUnit.setStatus(RoomUnitStatus.GESTURE, PetGestures.QUESTION.getKey());
        }
    }

    @Override
    public void serialize(ServerMessage message) {
        message.appendInt(this.id);
        message.appendString(this.name);
        if (this.petData != null) {
            message.appendInt(this.petData.getType());
        } else {
            message.appendInt(-1);
        }
        message.appendInt(this.race);
        message.appendString(this.color);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(0);
    }


    public void findNest() {
        HabboItem item = this.petData.randomNest(this.room.getRoomSpecialTypes().getNests());
        this.roomUnit.setCanWalk(true);
        if (item != null) {
            this.roomUnit.setGoalLocation(this.room.getLayout().getTile(item.getX(), item.getY()));
        } else {
            this.roomUnit.setStatus(RoomUnitStatus.LAY, this.room.getStackHeight(this.roomUnit.getX(), this.roomUnit.getY(), false) + "");
            this.say(this.petData.randomVocal(PetVocalsType.SLEEPING));
            this.task = PetTasks.DOWN;
        }
    }


    public void drink() {
        HabboItem item = this.petData.randomDrinkItem(this.room.getRoomSpecialTypes().getPetDrinks());
        if (item != null) {
            this.roomUnit.setCanWalk(true);
            this.roomUnit.setGoalLocation(this.room.getLayout().getTile(item.getX(), item.getY()));
        }
    }


    public void eat() {
        HabboItem item = this.petData.randomFoodItem(this.room.getRoomSpecialTypes().getPetFoods());
        {
            if (item != null) {
                this.roomUnit.setCanWalk(true);
                this.roomUnit.setGoalLocation(this.room.getLayout().getTile(item.getX(), item.getY()));
            }
        }
    }


    public void findToy() {
        HabboItem item = this.petData.randomToyItem(this.room.getRoomSpecialTypes().getPetToys());
        {
            if (item != null) {
                this.roomUnit.setCanWalk(true);
                this.roomUnit.setGoalLocation(this.room.getLayout().getTile(item.getX(), item.getY()));
            }
        }
    }


    public void randomHappyAction() {
        if (this.petData.actionsHappy.length > 0) {
            this.roomUnit.setStatus(RoomUnitStatus.fromString(this.petData.actionsHappy[Emulator.getRandom().nextInt(this.petData.actionsHappy.length)]), "");
        }
    }


    public void randomSadAction() {
        if (this.petData.actionsTired.length > 0) {
            this.roomUnit.setStatus(RoomUnitStatus.fromString(this.petData.actionsTired[Emulator.getRandom().nextInt(this.petData.actionsTired.length)]), "");
        }
    }


    public void randomAction() {
        if (this.petData.actionsRandom.length > 0) {
            this.roomUnit.setStatus(RoomUnitStatus.fromString(this.petData.actionsRandom[Emulator.getRandom().nextInt(this.petData.actionsRandom.length)]), "");
        }
    }


    public void addExperience(int amount) {
        this.experience += amount;

        if (this.room != null) {
            this.room.sendComposer(new RoomPetExperienceComposer(this, amount).compose());

            if(this.level < PetManager.experiences.length + 1 && this.experience >= PetManager.experiences[this.level - 1]) {
                this.levelUp();
            }
        }
    }


    protected void levelUp() {
            if (this.level >= PetManager.experiences.length + 1)
                return;

            if (this.experience > PetManager.experiences[this.level - 1]) {
                this.experience = PetManager.experiences[this.level - 1];
            }
            this.level++;
            this.say(this.petData.randomVocal(PetVocalsType.LEVEL_UP));
            this.addHappyness(100);
            this.roomUnit.setStatus(RoomUnitStatus.GESTURE, "exp");
            this.gestureTickTimeout = Emulator.getIntUnixTimestamp();
            AchievementManager.progressAchievement(Emulator.getGameEnvironment().getHabboManager().getHabbo(this.userId), Emulator.getGameEnvironment().getAchievementManager().getAchievement("PetLevelUp"));
            this.room.sendComposer(new PetLevelUpdatedComposer(this).compose());
        }


    public void addThirst(int amount) {
        this.levelThirst += amount;

        if (this.levelThirst > 100)
            this.levelThirst = 100;

        if (this.levelThirst < 0)
            this.levelThirst = 0;
    }


    public void addHunger(int amount) {
        this.levelHunger += amount;

        if (this.levelHunger > 100)
            this.levelHunger = 100;

        if (this.levelHunger < 0)
            this.levelHunger = 0;
    }


    public void freeCommand() {
        this.task = null;
        this.roomUnit.setGoalLocation(this.getRoomUnit().getCurrentLocation());
        this.roomUnit.clearStatus();
        this.roomUnit.setCanWalk(true);
        this.say(this.petData.randomVocal(PetVocalsType.GENERIC_NEUTRAL));
    }


    public void scratched(Habbo habbo) {
        this.addHappyness(10);
        this.addExperience(10);
        this.addRespect();
        this.needsUpdate = true;

        if (habbo != null) {
            habbo.getHabboStats().petRespectPointsToGive--;
            habbo.getHabboInfo().getCurrentRoom().sendComposer(new RoomPetRespectComposer(this).compose());

            AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("PetRespectGiver"));
        }

        AchievementManager.progressAchievement(Emulator.getGameEnvironment().getHabboManager().getHabbo(this.userId), Emulator.getGameEnvironment().getAchievementManager().getAchievement("PetRespectReceiver"));
    }


    public int getId() {
        return this.id;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Room getRoom() {
        return this.room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PetData getPetData() {
        return this.petData;
    }

    public void setPetData(PetData petData) {
        this.petData = petData;
    }

    public int getRace() {
        return this.race;
    }

    public void setRace(int race) {
        this.race = race;
    }

    public String getColor() {
        return this.color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getHappyness() {
        return this.happyness;
    }

    public void setHappyness(int happyness) {
        this.happyness = happyness;
    }

    public int getExperience() {
        return this.experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public int getEnergy() {
        return this.energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getMaxEnergy() {
        return this.level * 100;
    }

    public int getCreated() {
        return this.created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public RoomUnit getRoomUnit() {
        return this.roomUnit;
    }

    public void setRoomUnit(RoomUnit roomUnit) {
        this.roomUnit = roomUnit;
    }

    public PetTasks getTask() {
        return this.task;
    }

    public void setTask(PetTasks newTask) {
        this.task = newTask;
    }

    public boolean isMuted() {
        return this.muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public int getLevelThirst() {
        return this.levelThirst;
    }

    public void setLevelThirst(int levelThirst) {
        this.levelThirst = levelThirst;
    }

    public int getLevelHunger() {
        return this.levelHunger;
    }

    public void setLevelHunger(int levelHunger) {
        this.levelHunger = levelHunger;
    }

    public void removeFromRoom() {
        removeFromRoom(false);
    }

    public void removeFromRoom(boolean dontSendPackets) {

        if (this.roomUnit != null && this.roomUnit.getCurrentLocation() != null) {
            this.roomUnit.getCurrentLocation().removeUnit(this.roomUnit);
        }

        if (!dontSendPackets) {
            room.sendComposer(new RoomUserRemoveComposer(this.roomUnit).compose());
            room.removePet(this.id);
        }

        this.roomUnit = null;
        this.room = null;
        this.needsUpdate = true;
    }

    public int getStayStartedAt() {
        return stayStartedAt;
    }

    public void setStayStartedAt(int stayStartedAt) {
        this.stayStartedAt = stayStartedAt;
    }
}