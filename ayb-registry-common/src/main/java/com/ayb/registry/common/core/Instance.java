package com.ayb.registry.common.core;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 实例信息
 *
 * @author ayb
 * @date 2023/7/12
 */
@Data
public class Instance {
    /**
     * 命名空间
     */
    private String namespaceId;

    /**
     * ip地址
     */
    private String ip;

    /**
     * 端口
     */
    private String port;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 最后心跳时间
     */
    private volatile long lastBeat;

    /**
     * 是否健康
     */
    private Boolean healthy;

    /**
     * 拓展信息
     */
    private Map<String, String> metadata = new HashMap<String, String>();

    public String address() {
        return getIp() + ":" + getPort();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNamespaceId(), getIp(), getPort(), getServiceName());
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj || obj.getClass() != getClass()) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Instance other = (Instance) obj;

        return getIp().equals(other.getIp()) &&
                (Objects.equals(getPort(), other.getPort())) &&
                getNamespaceId().equals(other.getNamespaceId()) &&
                getServiceName().equals(other.getServiceName());
    }
}
