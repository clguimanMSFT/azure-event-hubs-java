/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.eventhubs;

import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstraction for a Scheduler functionality - which can later be replaced by a light-weight Thread
 */
public final class Timer {
    private static ScheduledThreadPoolExecutor executor = null;

    private static final Logger TRACE_LOGGER = LoggerFactory.getLogger(Timer.class);
    private static final HashSet<String> references = new HashSet<String>();
    private static final Object syncReferences = new Object();

    private Timer() {
    }

    /**
     * @param runFrequency implemented only for TimeUnit granularity - Seconds
     */
    public static ScheduledFuture<?> schedule(Runnable runnable, Duration runFrequency, TimerType timerType) {
        switch (timerType) {
            case OneTimeRun:
                return executor.schedule(runnable, runFrequency.toMillis(), TimeUnit.MILLISECONDS);

            case RepeatRun:
                return executor.scheduleWithFixedDelay(runnable, runFrequency.toMillis(), runFrequency.toMillis(), TimeUnit.MILLISECONDS);

            default:
                throw new UnsupportedOperationException("Unsupported timer pattern.");
        }
    }

    static void register(final String clientId) {
        synchronized (syncReferences) {
            if (references.size() == 0 && (executor == null || executor.isShutdown())) {
                final int corePoolSize = Math.max(Runtime.getRuntime().availableProcessors(), 4);
                if (TRACE_LOGGER.isInfoEnabled()) {
                    TRACE_LOGGER.info(
                            String.format(Locale.US, "Starting ScheduledThreadPoolExecutor with coreThreadPoolSize: %s", corePoolSize));
                }

                executor = new ScheduledThreadPoolExecutor(corePoolSize);
            }

            references.add(clientId);
        }
    }

    static void unregister(final String clientId) {
        synchronized (syncReferences) {
            if (references.remove(clientId) && references.size() == 0 && executor != null) {
                if (TRACE_LOGGER.isInfoEnabled()) {
                    TRACE_LOGGER.info("Shuting down ScheduledThreadPoolExecutor.");
                }

                executor.shutdownNow();
            }
        }
    }
}
