package com.ayb.registry.client;

import com.ayb.registry.client.core.RegistryService;
import com.ayb.registry.client.executor.GlobalExecutor;
import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.utils.HttpClientUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试
 *
 * @author ayb
 * @date 2023/7/28
 */
public class Test {
    public static void main(String[] args) throws Exception {
        List<String> servers = new ArrayList<>();
        servers.add("127.0.0.1:8888");

        RegistryService registryService = new RegistryService(servers);

        Instance instance = new Instance();
        instance.setNamespaceId("dev");
        instance.setServiceName("test-service");
        instance.setIp("127.0.0.1");
        instance.setPort("6666");

        registryService.registerInstance(instance);
        System.out.println(registryService.getInstanceList("dev", "test-service"));

        registryService.deregisterInstance(instance);
        System.out.println(registryService.getInstanceList("dev", "test-service"));

        HttpClientUtils.shutdownAll();
        GlobalExecutor.shutdownAll();
    }
}
