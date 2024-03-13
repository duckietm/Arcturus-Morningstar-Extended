package com.eu.habbo.messages.outgoing.hotelview;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class HotelViewCommunityGoalComposer extends MessageComposer {
    private final boolean achieved;
    private final int personalContributionScore;
    private final int personalRank;
    private final int totalAmount;
    private final int communityHighestAchievedLevel;
    private final int scoreRemainingUntilNextLevel;
    private final int percentCompletionTowardsNextLevel;
    private final String competitionName;
    private final int timeLeft;
    private final int[] rankData;

    public HotelViewCommunityGoalComposer(boolean achieved,
                                          int personalContributionScore,
                                          int personalRank,
                                          int totalAmount,
                                          int communityHighestAchievedLevel,
                                          int scoreRemainingUntilNextLevel,
                                          int percentCompletionTowardsNextLevel,
                                          String competitionName,
                                          int timeLeft,
                                          int[] rankData) {
        this.achieved = achieved;
        this.personalContributionScore = personalContributionScore;
        this.personalRank = personalRank;
        this.totalAmount = totalAmount;
        this.communityHighestAchievedLevel = communityHighestAchievedLevel;
        this.scoreRemainingUntilNextLevel = scoreRemainingUntilNextLevel;
        this.percentCompletionTowardsNextLevel = percentCompletionTowardsNextLevel;
        this.competitionName = competitionName;
        this.timeLeft = timeLeft;
        this.rankData = rankData;
    }

    //:test 1579 b:1 i:0 i:1 i:2 i:3 i:4 i:5 s:a i:6 i:1 i:1
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HotelViewCommunityGoalComposer);
        this.response.appendBoolean(this.achieved); //Achieved?
        this.response.appendInt(this.personalContributionScore); //User Amount
        this.response.appendInt(this.personalRank); //User Rank
        this.response.appendInt(this.personalRank); //Total Amount
        this.response.appendInt(this.totalAmount); //Community Highest Achieved
        this.response.appendInt(this.communityHighestAchievedLevel); //Community Score Untill Next Level
        this.response.appendInt(this.scoreRemainingUntilNextLevel); //Percent Completed Till Next Level
        this.response.appendString(this.competitionName);
        this.response.appendInt(this.timeLeft); //Timer
        this.response.appendInt(this.rankData.length); //Rank Count
        for (int i : this.rankData) {
            this.response.appendInt(i);
        }
        return this.response;
    }
}
