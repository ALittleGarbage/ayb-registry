package com.ayb.registry.server.core;

/**
 * @author ayb
 * @date 2023/7/13
 */
public interface RecordListener<T> {

    /**
     * 监听数据发生更改，执行此方法
     *
     * @param key
     * @param value
     */
    void onChange(String key, T value);
}
