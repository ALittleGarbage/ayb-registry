package com.ayb.registry.server.executor;

import com.ayb.registry.server.utils.SpringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors() <= 1 ? 1 : Runtime.getRuntime().availableProcessors() / 2;

    private static final ScheduledExecutorService REFRESH_HEALTH_EXECUTOR;

    private static final ScheduledExecutorService CHECK_HEALTH_EXECUTOR;

    private static final ScheduledExecutorService NOTIFY_EXECUTOR;

    private static final ScheduledExecutorService PUSH_SERVICE_EXECUTOR;

    private static final ScheduledExecutorService CHECK_CLIENT_EXECUTOR;

    private static final ScheduledExecutorService CLUSTER_SYNC_EXECUTOR;

    private static final ScheduledExecutorService LOAD_DATA_EXECUTOR;

    private static final ScheduledExecutorService CLUSTER_BEAT_EXECUTOR;
    public volatile static boolean isShutDown = false;

    static {
        // 集群相关
        CLUSTER_BEAT_EXECUTOR = Executors.newScheduledThreadPool(1, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(CLUSTER_BEAT_EXECUTOR);
        CLUSTER_SYNC_EXECUTOR = Executors.newScheduledThreadPool(1, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(CLUSTER_SYNC_EXECUTOR);
        LOAD_DATA_EXECUTOR = Executors.newScheduledThreadPool(1, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(LOAD_DATA_EXECUTOR);

        /**
         * 客户端相关
         */
        // 检查客户端
        CHECK_CLIENT_EXECUTOR = Executors.newScheduledThreadPool(1, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(CHECK_CLIENT_EXECUTOR);
        PUSH_SERVICE_EXECUTOR = Executors.newScheduledThreadPool(DEFAULT_THREAD_COUNT, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(PUSH_SERVICE_EXECUTOR);
        NOTIFY_EXECUTOR = Executors.newScheduledThreadPool(1, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(NOTIFY_EXECUTOR);
        CHECK_HEALTH_EXECUTOR = Executors.newScheduledThreadPool(DEFAULT_THREAD_COUNT, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(CHECK_HEALTH_EXECUTOR);
        REFRESH_HEALTH_EXECUTOR = Executors.newScheduledThreadPool(DEFAULT_THREAD_COUNT, Executors.defaultThreadFactory());
        EXECUTOR_SERVICE_SET.add(REFRESH_HEALTH_EXECUTOR);

        SpringUtils.shutdownHook(new Thread(GlobalExecutor::shutdownAll));
    }

    public static void scheduleRefreshHealth(Runnable task) {
        REFRESH_HEALTH_EXECUTOR.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    public static void scheduleCheckHealth(Runnable task) {
        CHECK_HEALTH_EXECUTOR.scheduleWithFixedDelay(task, 5 * 1000, 5 * 1000, TimeUnit.MILLISECONDS);
    }

    public static void submitNotifyTask(Runnable task) {
        NOTIFY_EXECUTOR.submit(task);
    }

    public static Future<?> schedulePushService(Runnable task) {
        return PUSH_SERVICE_EXECUTOR.schedule(task, 1000, TimeUnit.MILLISECONDS);
    }

    public static void scheduleCheckZombieClient(Runnable task) {
        CHECK_CLIENT_EXECUTOR.scheduleWithFixedDelay(task, 0, 20, TimeUnit.SECONDS);
    }

    public static void scheduleClusterSyncData(Runnable task) {
        CLUSTER_SYNC_EXECUTOR.submit(task);
    }

    public static void submitLoadDataTask(Runnable task) {
        LOAD_DATA_EXECUTOR.submit(task);
    }

    public static void scheduleClusterBeatTask(Runnable task) {
        CLUSTER_BEAT_EXECUTOR.scheduleWithFixedDelay(task, 5 * 1000, 5 * 1000, TimeUnit.MILLISECONDS);
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
