package com.ayb.registry.server.consistency.distro;

/**
 * 用于获取同步数据
 *
 * @author ayb
 * @date 2023/7/21
 */
public interface DistroDataStorage {

    /**
     * 获取指定的同步数据
     *
     * @param key
     * @return
     */
    byte[] getDistroData(String key);

    /**
     * 获取全部的同步数据快照
     *
     * @return
     */
    byte[] getDataSnapshot();

}
