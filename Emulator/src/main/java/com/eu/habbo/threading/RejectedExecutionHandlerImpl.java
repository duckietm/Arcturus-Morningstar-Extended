package com.eu.habbo.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RejectedExecutionHandlerImpl.class);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        LOGGER.error(r.toString() + " is rejected");
    }
}
