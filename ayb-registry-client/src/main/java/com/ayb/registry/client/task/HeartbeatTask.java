package com.ayb.registry.client.task;

import com.ayb.registry.client.core.ClientProxy;
import com.ayb.registry.common.core.Instance;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 发送实例心跳任务
 *
 * @author ayb
 * @date 2023/7/27
 */
@Slf4j
public class HeartbeatTask implements Runnable {

    private Set<Instance> instances = ConcurrentHashMap.newKeySet();

    private List<String> servers;

    public HeartbeatTask(List<String> servers) {
        this.servers = servers;
    }

    public void addInstance(Instance instance) {
        instances.add(instance);
    }

    public void removeInstance(Instance instance) {
        instances.remove(instance);
    }

    @Override
    public void run() {
        String serverAddress = null;
        try {
            for (Instance instance : instances) {
                int index = ClientProxy.hash(instance.getServiceName()) % servers.size();
                serverAddress = servers.get(index);
                boolean result = ClientProxy.sendHeartbeat(instance, serverAddress);
                if (!result) {
                    log.error("向{}地址发送心跳失败", serverAddress);
                }
            }
        } catch (Exception e) {
            log.error("向{}地址发送心跳失败,原因:{}", serverAddress, e.getMessage());
        }
    }
}
