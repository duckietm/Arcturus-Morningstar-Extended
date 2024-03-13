package com.eu.habbo.core;

import com.eu.habbo.Emulator;

public class Scheduler implements Runnable {
    protected boolean disposed;
    protected int interval;

    public Scheduler(int interval) {
        this.interval = interval;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    public void setDisposed(boolean disposed) {
        this.disposed = disposed;
    }

    public int getInterval() {
        return this.interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    @Override
    public void run() {
        if (this.disposed)
            return;

        Emulator.getThreading().run(this, this.interval * 1000);
    }
}