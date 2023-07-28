package com.ayb.registry.server.consistency.distro;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.utils.JsonUtils;
import com.ayb.registry.server.consistency.ConsistencyService;
import com.ayb.registry.server.consistency.distro.component.DistroProtocol;
import com.ayb.registry.server.core.RecordListener;
import com.ayb.registry.server.core.Service;
import com.ayb.registry.server.executor.GlobalExecutor;
import com.ayb.registry.server.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 用于数据操作
 *
 * @author ayb
 * @date 2023/7/14
 */
@Slf4j
@org.springframework.stereotype.Service
public class DistroConsistencyServiceImpl implements ConsistencyService, DistroDataProcessor, DistroDataStorage {

    private Map<String, RecordListener> listeners = new ConcurrentHashMap<>();

    private Map<String, List<Instance>> dataStore = new ConcurrentHashMap<>(1024);

    private Notifier notifier = new Notifier();

    /**
     * 初始化，执行通知任务
     */
    @PostConstruct
    public void init() {
        GlobalExecutor.submitNotifyTask(notifier);
    }

    @Override
    public void put(String key, List<Instance> value) {
        onPut(key, value);
        DistroProtocol distroProtocol = SpringUtils.getBean(DistroProtocol.class);
        // 将数据同步到别的集群节点中
        distroProtocol.sync(key);
    }

    @Override
    public void listen(String key, RecordListener listener) {
        if (!listeners.containsKey(key)) {
            listeners.put(key, listener);
        }
    }

    @Override
    public void processData(String key, List<Instance> instances) {
        onPut(key, instances);
    }

    @Override
    public boolean processSnapshot(byte[] data) {
        RecordListener serviceManagerListener = listeners.get("ServiceManager");
        if (serviceManagerListener == null) {
            return false;
        }
        byte[] decode = Base64.getDecoder().decode(data);
        Map<String, List<Instance>> dataMap = JsonUtils.toMap(new String(decode));

        for (Map.Entry<String, List<Instance>> entry : dataMap.entrySet()) {
            dataStore.put(entry.getKey(), entry.getValue());
            if (!listeners.containsKey(entry.getKey())) {
                Service service = new Service();
                String[] split = entry.getKey().split("##");
                service.setNamespaceId(split[0]);
                service.setName(split[1]);

                serviceManagerListener.onChange(entry.getKey(), service);
            }
        }

        for (Map.Entry<String, List<Instance>> entry : dataMap.entrySet()) {
            if (!listeners.containsKey(entry.getKey())) {
                continue;
            }
            RecordListener listener = listeners.get(entry.getKey());
            listener.onChange(entry.getKey(), entry.getValue());
        }

        return true;
    }

    private void onPut(String key, List<Instance> value) {
        dataStore.put(key, value);
        if (!listeners.containsKey(key)) {
            return;
        }
        notifier.addTask(key);
    }


    @Override
    public byte[] getDistroData(String key) {
        Map<String, List<Instance>> result = new HashMap<>(1);
        result.put(key, (List<Instance>) dataStore.get(key));
        return JsonUtils.toJsonByte(result);
    }

    /**
     * 获取全部的service数据
     *
     * @return
     */
    @Override
    public byte[] getDataSnapshot() {
        if (MapUtils.isEmpty(dataStore)) {
            return new byte[0];
        }
        return JsonUtils.toJsonByte(dataStore);
    }

    /**
     * 执行更改service的任务
     */
    private class Notifier implements Runnable {

        private Set<String> services = ConcurrentHashMap.newKeySet(1024);

        private BlockingQueue<String> tasks = new LinkedBlockingQueue<>(2 * 1024);

        public void addTask(String key) {

            if (services.contains(key)) {
                return;
            }
            services.add(key);
            tasks.offer(key);
        }

        @Override
        public void run() {
            for (; ; ) {
                try {
                    String key = tasks.take();
                    handle(key);
                } catch (InterruptedException e) {
                    log.error("获取task出错,原因:{}", e.getMessage());
                }
            }
        }

        /**
         * 执行对应的监听的onChanged方法
         *
         * @param key
         */
        private void handle(String key) {
            services.remove(key);

            if (!listeners.containsKey(key)) {
                return;
            }

            RecordListener listener = listeners.get(key);
            listener.onChange(key, dataStore.get(key));
        }
    }
}

