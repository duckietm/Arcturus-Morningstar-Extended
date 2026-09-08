package com.eu.habbo.habbohotel.quests;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** A user's progress on one reward track: `users_reward_tracks` with its task counts and claims. */
public final class UserRewardTrackState {
    private final String trackId;
    private int points;
    private boolean premium;
    private final Map<String, Integer> taskProgress = new ConcurrentHashMap<>();
    private final Set<String> claimedPrizes = ConcurrentHashMap.newKeySet();

    public UserRewardTrackState(String trackId, int points, boolean premium) {
        this.trackId = trackId;
        this.points = Math.max(0, points);
        this.premium = premium;
    }

    public String getTrackId() {
        return this.trackId;
    }

    public int getPoints() {
        return this.points;
    }

    public void addPoints(int points) {
        this.points = Math.max(0, this.points + points);
    }

    public boolean isPremium() {
        return this.premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public int progressOf(String taskId) {
        return this.taskProgress.getOrDefault(taskId, 0);
    }

    public void setProgress(String taskId, int count) {
        this.taskProgress.put(taskId, Math.max(0, count));
    }

    public boolean isClaimed(String prizeId) {
        return this.claimedPrizes.contains(prizeId);
    }

    public void markClaimed(String prizeId) {
        this.claimedPrizes.add(prizeId);
    }

    public boolean isPrizeLocked(RewardTrack.Prize prize) {
        return prize.isPremium() && !this.premium;
    }

    public boolean isPrizeAvailable(RewardTrack.Prize prize) {
        return !this.isPrizeLocked(prize) && this.points >= prize.getRequiredPoints();
    }

    public boolean isPrizeClaimable(RewardTrack.Prize prize) {
        return this.isPrizeAvailable(prize) && !this.isClaimed(prize.getId());
    }
}
