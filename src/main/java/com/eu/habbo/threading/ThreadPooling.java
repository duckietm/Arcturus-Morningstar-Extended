package com.eu.habbo.threading;

import com.eu.habbo.Emulator;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadPooling {

    public final int threads;
    private final ScheduledExecutorService scheduledPool;
    private volatile boolean canAdd;


    public ThreadPooling(Integer threads) {
        this.threads = threads;
        this.scheduledPool = new HabboExecutorService(this.threads, new DefaultThreadFactory("HabExec"));
        this.canAdd = true;
        log.info("Thread Pool -> Loaded!");
    }

    public ScheduledFuture run(Runnable run) {
        try {
            if (this.canAdd) {
                return this.run(run, 0);
            } else {
                if (Emulator.isShuttingDown) {
                    run.run();
                }
            }
        } catch (Exception e) {
            log.error("Caught exception", e);
        }

        return null;
    }

    public ScheduledFuture run(Runnable run, long delay) {
        try {
            if (this.canAdd) {
                return this.scheduledPool.schedule(() -> {
                    try {
                        run.run();
                    } catch (Exception e) {
                        log.error("Caught exception", e);
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error("Caught exception", e);
        }

        return null;
    }

    public void shutDown() {
        this.canAdd = false;
        this.scheduledPool.shutdownNow();

        log.info("Threading -> Disposed!");
    }

    public void setCanAdd(boolean canAdd) {
            this.canAdd = canAdd;
    }

    public ScheduledExecutorService getService() {
        return this.scheduledPool;
    }


}
