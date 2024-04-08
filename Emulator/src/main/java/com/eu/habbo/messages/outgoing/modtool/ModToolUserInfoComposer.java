package com.eu.habbo.messages.outgoing.modtool;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.modtool.ModToolSanctionItem;
import com.eu.habbo.habbohotel.modtool.ModToolSanctionLevelItem;
import com.eu.habbo.habbohotel.modtool.ModToolSanctions;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;

public class ModToolUserInfoComposer extends MessageComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModToolUserInfoComposer.class);

    private final ResultSet set;

    public ModToolUserInfoComposer(ResultSet set) {
        this.set = set;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ModToolUserInfoComposer);
        try {
            int totalBans = 0;

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) AS amount FROM bans WHERE user_id = ?")) {
                statement.setInt(1, this.set.getInt("user_id"));
                try (ResultSet set = statement.executeQuery()) {
                    if (set.next()) {
                       totalBans = set.getInt("amount");
                    }
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }

            this.response.appendInt(this.set.getInt("user_id"));
            this.response.appendString(this.set.getString("username"));
            this.response.appendString(this.set.getString("look"));
            this.response.appendInt((Emulator.getIntUnixTimestamp() - this.set.getInt("account_created")) / 60);
            this.response.appendInt((this.set.getInt("online") == 1 ? 0 : Emulator.getIntUnixTimestamp() - this.set.getInt("last_online")) / 60);
            this.response.appendBoolean(this.set.getInt("online") == 1);
            this.response.appendInt(this.set.getInt("cfh_send"));
            this.response.appendInt(this.set.getInt("cfh_abusive"));
            this.response.appendInt(this.set.getInt("cfh_warnings"));
            this.response.appendInt(totalBans); // Number of bans
            this.response.appendInt(this.set.getInt("tradelock_amount"));
            this.response.appendString(""); //Trading lock expiry timestamp
            this.response.appendString(""); //Last Purchase Timestamp
            this.response.appendInt(this.set.getInt("user_id")); //Personal Identification #
            this.response.appendInt(0); // Number of account bans
            this.response.appendString(this.set.getBoolean("hide_mail") ? "" : this.set.getString("mail"));
            this.response.appendString("Rank (" + this.set.getInt("rank_id") + "): " + this.set.getString("rank_name")); //user_class_txt

            ModToolSanctions modToolSanctions = Emulator.getGameEnvironment().getModToolSanctions();

            if (Emulator.getConfig().getBoolean("hotel.sanctions.enabled")) {
                THashMap<Integer, ArrayList<ModToolSanctionItem>> modToolSanctionItemsHashMap = Emulator.getGameEnvironment().getModToolSanctions().getSanctions(this.set.getInt("user_id"));
                ArrayList<ModToolSanctionItem> modToolSanctionItems = modToolSanctionItemsHashMap.get(this.set.getInt("user_id"));

                if (modToolSanctionItems != null && modToolSanctionItems.size() > 0) //has sanction
                {
                    ModToolSanctionItem item = modToolSanctionItems.get(modToolSanctionItems.size() - 1);
                    ModToolSanctionLevelItem modToolSanctionLevelItem = modToolSanctions.getSanctionLevelItem(item.sanctionLevel);

                    this.response.appendString(modToolSanctions.getSanctionType(modToolSanctionLevelItem));
                    this.response.appendInt(31);
                }

            }

            return this.response;
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
        return null;
    }
}
