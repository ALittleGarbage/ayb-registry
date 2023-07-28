package com.ayb.registry.common.core;

import lombok.Data;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * 客户端信息
 *
 * @author ayb
 * @date 2023/7/19
 */
@Data
public class PushClient {
    private String namespaceId;

    private String serviceName;

    private String ip;

    private Integer port;

    private long lastBeat = System.currentTimeMillis();

    public void refresh() {
        lastBeat = System.currentTimeMillis();
    }

    public String getAddress() {
        return ip + ":" + port;
    }

    public boolean isZombie() {
        return System.currentTimeMillis() - lastBeat > 10 * 1000;
    }

    public InetSocketAddress getSocketAddress() {
        return InetSocketAddress.createUnresolved(ip, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PushClient that = (PushClient) o;
        return Objects.equals(namespaceId, that.namespaceId) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(ip, that.ip) &&
                Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespaceId, serviceName, ip, port);
    }
}
