package com.ayb.registry.client.executor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 全局线程池
 *
 * @author ayb
 * @date 2023/7/14
 */
public class GlobalExecutor {

    private static final Set<ScheduledExecutorService> EXECUTOR_SERVICE_SET = new HashSet<>();

    private static final ScheduledExecutorService SEND_HEARTBEAT_EXECUTOR;

    private static final ScheduledExecutorService SEND_CLIENT_EXECUTOR;

    public volatile static boolean isShutDown = false;

    static {
        SEND_CLIENT_EXECUTOR = Executors.newScheduledThreadPool(1, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(SEND_CLIENT_EXECUTOR);

        SEND_HEARTBEAT_EXECUTOR = Executors.newScheduledThreadPool(1, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(SEND_HEARTBEAT_EXECUTOR);
    }


    public static void scheduleSendHeartBeat(Runnable task) {
        SEND_HEARTBEAT_EXECUTOR.scheduleWithFixedDelay(task, 5 * 1000, 5 * 1000, TimeUnit.MILLISECONDS);
    }

    public static void scheduleSendClient(Runnable task) {
        SEND_CLIENT_EXECUTOR.scheduleWithFixedDelay(task, 5 * 1000, 5 * 1000, TimeUnit.MILLISECONDS);
    }

    public static void shutdownAll() {
        isShutDown = true;
        for (ScheduledExecutorService executorService : EXECUTOR_SERVICE_SET) {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }
}
