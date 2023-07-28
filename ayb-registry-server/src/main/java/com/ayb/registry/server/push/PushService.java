package com.ayb.registry.server.push;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.core.PushClient;
import com.ayb.registry.common.udp.UdpSocket;
import com.ayb.registry.common.utils.JsonUtils;
import com.ayb.registry.server.core.Service;
import com.ayb.registry.server.executor.GlobalExecutor;
import com.ayb.registry.server.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

//TODO 测试主动推送

/**
 * 用于发布更改的service信息
 *
 * @author ayb
 * @date 2023/7/13
 */
@Slf4j
@Component
public class PushService implements ApplicationListener<ServiceChangeEvent> {

    /**
     * Map<"namespaceId##serviceName", Map<"ip:port", Client>>
     */
    private final ConcurrentMap<String, ConcurrentMap<String, PushClient>> clientMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Future<?>> futureMap = new ConcurrentHashMap<>();
    private UdpSocket udpSocket = new UdpSocket();

    @PostConstruct
    public void init() {
        // 定时检查客户端健康状态
        GlobalExecutor.scheduleCheckZombieClient(this::removeZombieClient);
    }

    /**
     * 移除僵尸Client
     */
    private void removeZombieClient() {
        for (Map.Entry<String, ConcurrentMap<String, PushClient>> entry : clientMap.entrySet()) {
            ConcurrentMap<String, PushClient> clients = entry.getValue();
            for (Map.Entry<String, PushClient> en : clients.entrySet()) {
                PushClient client = en.getValue();
                if (client.isZombie()) {
                    clients.remove(en.getKey());
                }
            }
        }
    }

    /**
     * 监听service，将更新的service推送到客户端
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ServiceChangeEvent event) {
        Service service = event.getService();
        String serviceName = service.getName();
        String namespaceId = service.getNamespaceId();
        String key = namespaceId + "##" + serviceName;
        List<Instance> allIp = service.getAllIp();

        String jsonData = JsonUtils.toJson(allIp);
        byte[] byteData = jsonData.getBytes(StandardCharsets.UTF_8);

        Future<?> future = GlobalExecutor.schedulePushService(() -> {
            try {
                ConcurrentMap<String, PushClient> clients = clientMap.get(key);
                if (MapUtils.isEmpty(clients)) {
                    return;
                }

                for (PushClient client : clients.values()) {
                    if (client.isZombie()) {
                        clients.remove(client.getAddress());
                        continue;
                    }

                    udpSocket.send(byteData, client.getSocketAddress());
                }
            } catch (Exception e) {
                log.error("推送服务时发生错误,原因:{}", e.getMessage());
            } finally {
                futureMap.remove(key);
            }
        });

        futureMap.put(key, future);
    }

    /**
     * 发布service更改事件
     *
     * @param service
     */
    public void serviceChanged(Service service) {
        SpringUtils.publishEvent(new ServiceChangeEvent(this, service));
    }

    /**
     * 添加或者刷新客户端心跳
     *
     * @param pushClient
     */
    public void addOrRefreshClient(PushClient pushClient) {
        String key = pushClient.getNamespaceId() + "##" + pushClient.getServiceName();

        ConcurrentMap<String, PushClient> clients = clientMap.get(key);
        if (clients == null) {
            clients = new ConcurrentHashMap<>(1024);
            ConcurrentMap<String, PushClient> ifAbsent = clientMap.putIfAbsent(key, clients);
            if (ifAbsent != null) {
                clients = ifAbsent;
            }
        }

        PushClient client = clients.get(pushClient.getAddress());
        if (client != null) {
            client.refresh();
        } else {
            clients.put(pushClient.getAddress(), pushClient);
        }
    }


}
