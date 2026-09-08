package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.List;

/** DailyTasksAdded (670): tasks that appeared while the window was open (a bonus task unlocking). */
public class DailyTasksAddedComposer extends MessageComposer {
    private final List<ActiveDailyTasksComposer.Task> tasks;

    public DailyTasksAddedComposer(List<ActiveDailyTasksComposer.Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.DailyTasksAddedComposer);
        this.response.appendInt(this.tasks.size());
        for (ActiveDailyTasksComposer.Task task : this.tasks) {
            this.response.append(task);
        }
        return this.response;
    }

    public List<ActiveDailyTasksComposer.Task> getTasks() {
        return tasks;
    }
}
