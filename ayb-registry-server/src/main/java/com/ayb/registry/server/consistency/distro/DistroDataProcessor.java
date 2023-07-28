package com.ayb.registry.server.consistency.distro;

import com.ayb.registry.common.core.Instance;

import java.util.List;

/**
 * 用于载入同步数据
 *
 * @author ayb
 * @date 2023/7/21
 */
public interface DistroDataProcessor {

    /**
     * 载入特定的同步数据
     *
     * @param key
     * @param instances
     */
    void processData(String key, List<Instance> instances);

    /**
     * 载入从别的集群获得的全部数据快照
     *
     * @param data
     * @return
     */
    boolean processSnapshot(byte[] data);
}
