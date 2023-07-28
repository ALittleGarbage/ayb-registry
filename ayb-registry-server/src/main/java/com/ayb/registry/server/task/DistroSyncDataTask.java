package com.ayb.registry.server.task;

import com.ayb.registry.server.consistency.distro.DistroDataStorage;
import com.ayb.registry.server.consistency.distro.component.DistroHttpClient;
import com.ayb.registry.server.executor.GlobalExecutor;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 数据发生变更时，将数据同步到别的集群
 *
 * @author ayb
 * @date 2023/7/21
 */
@Slf4j
public class DistroSyncDataTask implements Runnable {

    private DistroDataStorage distroDataStorage;

    private BlockingQueue<Pair<String, String>> tasks;

    public DistroSyncDataTask(DistroDataStorage distroDataStorage) {
        this.distroDataStorage = distroDataStorage;
        tasks = new LinkedBlockingQueue<>();
    }

    public void addTask(String key, String serverAddress) {
        try {
            tasks.put(new Pair<>(key, serverAddress));
        } catch (InterruptedException ire) {
            log.error(ire.toString(), ire);
        }
    }

    @Override
    public void run() {
        while (!GlobalExecutor.isShutDown) {
            try {
                Pair<String, String> task = tasks.take();
                String key = task.getKey();
                String serverAddress = task.getValue();
                byte[] data = distroDataStorage.getDistroData(key);

                DistroHttpClient.syncData(data, serverAddress);
            } catch (Throwable e) {
                log.error("同步数据时发生错误,原因:{}", e.getCause().getMessage());
            }
        }
    }

}
