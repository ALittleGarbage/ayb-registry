package com.ayb.registry.server.consistency;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.server.core.RecordListener;

import java.util.List;

/**
 * @author ayb
 * @date 2023/7/14
 */
public interface ConsistencyService {

    /**
     * 推送将更改的service信息，
     *
     * @param key
     * @param value
     */
    void put(String key, List<Instance> value);

    /**
     * 放置监听
     *
     * @param key
     * @param listener
     */
    void listen(String key, RecordListener<?> listener);
}
