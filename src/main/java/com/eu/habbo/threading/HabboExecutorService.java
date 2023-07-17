package com.eu.habbo.threading;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class HabboExecutorService extends ScheduledThreadPoolExecutor {
    public HabboExecutorService(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if (t != null && !(t instanceof IOException)) {
            log.error("Error in HabboExecutorService", t);
        }
    }
}
