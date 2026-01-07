package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.polls.infobus.SimplePollAnswerComposer;
import com.eu.habbo.messages.outgoing.polls.infobus.SimplePollStartComposer;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages word quizzes/polls within a room.
 */
public class RoomWordQuizManager {
    private final Room room;
    private final List<Integer> userVotes;
    
    private String wordQuiz = "";
    private int noVotes = 0;
    private int yesVotes = 0;
    private int wordQuizEnd = 0;

    public RoomWordQuizManager(Room room) {
        this.room = room;
        this.userVotes = new ArrayList<>();
    }

    /**
     * Handles a user's quiz answer.
     */
    public void handleWordQuiz(Habbo habbo, String answer) {
        synchronized (this.userVotes) {
            if (!this.wordQuiz.isEmpty() && !this.hasVotedInWordQuiz(habbo)) {
                answer = answer.replace(":", "");

                if (answer.equals("0")) {
                    this.noVotes++;
                } else if (answer.equals("1")) {
                    this.yesVotes++;
                }

                this.room.sendComposer(
                    new SimplePollAnswerComposer(habbo.getHabboInfo().getId(), answer, this.noVotes,
                        this.yesVotes).compose());
                this.userVotes.add(habbo.getHabboInfo().getId());
            }
        }
    }

    /**
     * Starts a word quiz.
     */
    public void startWordQuiz(String question, int duration) {
        if (!this.hasActiveWordQuiz()) {
            this.wordQuiz = question;
            this.noVotes = 0;
            this.yesVotes = 0;
            this.userVotes.clear();
            this.wordQuizEnd = Emulator.getIntUnixTimestamp() + (duration / 1000);
            this.room.sendComposer(new SimplePollStartComposer(duration, question).compose());
        }
    }

    /**
     * Checks if there is an active word quiz.
     */
    public boolean hasActiveWordQuiz() {
        return Emulator.getIntUnixTimestamp() < this.wordQuizEnd;
    }

    /**
     * Checks if a user has voted in the current quiz.
     */
    public boolean hasVotedInWordQuiz(Habbo habbo) {
        return this.userVotes.contains(habbo.getHabboInfo().getId());
    }

    /**
     * Resets the quiz state.
     */
    public void reset() {
        this.wordQuiz = "";
        this.yesVotes = 0;
        this.noVotes = 0;
        this.userVotes.clear();
    }

    // Getters and setters for backward compatibility
    public String getWordQuiz() {
        return wordQuiz;
    }

    public void setWordQuiz(String wordQuiz) {
        this.wordQuiz = wordQuiz;
    }

    public int getNoVotes() {
        return noVotes;
    }

    public void setNoVotes(int noVotes) {
        this.noVotes = noVotes;
    }

    public int getYesVotes() {
        return yesVotes;
    }

    public void setYesVotes(int yesVotes) {
        this.yesVotes = yesVotes;
    }

    public int getWordQuizEnd() {
        return wordQuizEnd;
    }

    public void setWordQuizEnd(int wordQuizEnd) {
        this.wordQuizEnd = wordQuizEnd;
    }

    public List<Integer> getUserVotes() {
        return userVotes;
    }
}
