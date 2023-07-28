package com.ayb.registry.client.task;

import com.ayb.registry.client.core.ClientProxy;
import com.ayb.registry.common.core.PushClient;
import com.ayb.registry.common.udp.UdpSocket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TODO
 * 发送客户端订阅信息任务
 *
 * @author ayb
 * @date 2023/7/28
 */
@Slf4j
public class SubscribeTask implements Runnable {

    private ConcurrentMap<String, PushClient> clientMap = new ConcurrentHashMap<>();

    private ConcurrentMap<String, UdpSocket> udpMap = new ConcurrentHashMap<>();

    private List<String> servers;

    public SubscribeTask(List<String> servers) {
        this.servers = servers;
    }

    public void addClient(String namespaceId, String serviceName) {
        String key = namespaceId + "##" + serviceName;
        if (clientMap.containsKey(key)) {
            return;
        }

        UdpSocket udpSocket = new UdpSocket();
        InetSocketAddress address = udpSocket.address();

        PushClient pushClient = new PushClient();
        pushClient.setNamespaceId(namespaceId);
        pushClient.setServiceName(serviceName);
        pushClient.setIp(address.getHostName());
        pushClient.setPort(address.getPort());

        clientMap.put(key, pushClient);
        udpMap.putIfAbsent(key, udpSocket);
    }

    public void removeClient(String namespaceId, String serviceName) {
        String key = namespaceId + "##" + serviceName;
        clientMap.remove(key);
        udpMap.remove(key);
    }

    @Override
    public void run() {
        String serverAddress = null;

        try {
            for (PushClient client : clientMap.values()) {
                serverAddress = servers.get(ClientProxy.hash(client.getServiceName()));
                boolean result = ClientProxy.sendClientInfo(client, serverAddress);
                if (!result) {
                    log.error("向{}地址推送客户端订阅信息时发生错误", serverAddress);
                }
            }
        } catch (Exception e) {
            log.error("向{}地址推送客户端订阅信息时发生错误,原因:{}", serverAddress, e.getMessage());
        }
    }
}
