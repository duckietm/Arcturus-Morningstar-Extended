package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.messages.ISerialize;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class QuestsComposer extends MessageComposer {
    private final List<Quest> quests;
    private final boolean unknownBoolean;

    public QuestsComposer(List<Quest> quests, boolean unknownBoolean) {
        this.quests = quests;
        this.unknownBoolean = unknownBoolean;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestsComposer);
        this.response.appendInt(this.quests.size());
        for (Quest quest : this.quests) {
            this.response.append(quest);
        }
        this.response.appendBoolean(this.unknownBoolean);
        return this.response;
    }

    public static class Quest implements ISerialize {
        private final String campaignCode;
        private final int completedQuestsInCampaign;
        private final int questCountInCampaign;
        private final int activityPointType;
        private final int id;
        private final boolean accepted;
        private final String type;
        private final String imageVersion;
        private final int rewardCurrencyAmount;
        private final String localizationCode;
        private final int completedSteps;
        private final int totalSteps;
        private final int sortOrder;
        private final String catalogPageName;
        private final String chainCode;
        private final boolean easy;

        public Quest(String campaignCode, int completedQuestsInCampaign, int questCountInCampaign, int activityPointType, int id, boolean accepted, String type, String imageVersion, int rewardCurrencyAmount, String localizationCode, int completedSteps, int totalSteps, int sortOrder, String catalogPageName, String chainCode, boolean easy) {
            this.campaignCode = campaignCode;
            this.completedQuestsInCampaign = completedQuestsInCampaign;
            this.questCountInCampaign = questCountInCampaign;
            this.activityPointType = activityPointType;
            this.id = id;
            this.accepted = accepted;
            this.type = type;
            this.imageVersion = imageVersion;
            this.rewardCurrencyAmount = rewardCurrencyAmount;
            this.localizationCode = localizationCode;
            this.completedSteps = completedSteps;
            this.totalSteps = totalSteps;
            this.sortOrder = sortOrder;
            this.catalogPageName = catalogPageName;
            this.chainCode = chainCode;
            this.easy = easy;
        }

        @Override
        public void serialize(ServerMessage message) {
            message.appendString(this.campaignCode);
            message.appendInt(this.completedQuestsInCampaign);
            message.appendInt(this.questCountInCampaign);
            message.appendInt(this.activityPointType);
            message.appendInt(this.id);
            message.appendBoolean(this.accepted);
            message.appendString(this.type);
            message.appendString(this.imageVersion);
            message.appendInt(this.rewardCurrencyAmount);
            message.appendString(this.localizationCode);
            message.appendInt(this.completedSteps);
            message.appendInt(this.totalSteps);
            message.appendInt(this.sortOrder);
            message.appendString(this.catalogPageName);
            message.appendString(this.chainCode);
            message.appendBoolean(this.easy);
        }
    }
}