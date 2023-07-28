package com.ayb.registry.client.core;

import com.ayb.registry.client.executor.GlobalExecutor;
import com.ayb.registry.client.task.HeartbeatTask;
import com.ayb.registry.common.core.Instance;

import java.util.List;

/**
 * 注册与发现
 *
 * @author ayb
 * @date 2023/7/27
 */
public class RegistryService {

    private List<String> servers;

    private HeartbeatTask heartbeatTask;

    public RegistryService(List<String> servers) {
        this.servers = servers;
        heartbeatTask = new HeartbeatTask(servers);

        GlobalExecutor.scheduleSendHeartBeat(heartbeatTask);
        // TODO 添加客户端心跳 udp接收
        //GlobalExecutor.scheduleSendClient();
    }

    public void registerInstance(Instance instance) {
        int index = ClientProxy.hash(instance.getServiceName()) % servers.size();
        boolean result = ClientProxy.register(instance, servers.get(index));
        if (result) {
            heartbeatTask.addInstance(instance);
        }
    }

    public void deregisterInstance(Instance instance) {
        int index = ClientProxy.hash(instance.getServiceName()) % servers.size();
        boolean result = ClientProxy.deregister(instance, servers.get(index));
        if (result) {
            heartbeatTask.removeInstance(instance);
        }
    }

    public List<Instance> getInstanceList(String namespaceId, String serviceName) {
        int index = ClientProxy.hash(serviceName) % servers.size();
        return ClientProxy.getServiceList(namespaceId, serviceName, servers.get(index));
    }
}
