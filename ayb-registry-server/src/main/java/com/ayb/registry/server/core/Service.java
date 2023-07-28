package com.ayb.registry.server.core;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.server.executor.GlobalExecutor;
import com.ayb.registry.server.push.PushService;
import com.ayb.registry.server.task.HealthCheckTask;
import com.ayb.registry.server.task.RefreshBeatTask;
import com.ayb.registry.server.utils.SpringUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理serviceName信息
 *
 * @author ayb
 * @date 2023/7/13
 */
@Data
public class Service implements RecordListener<List<Instance>> {

    private String namespaceId;

    private String name;

    private Set<Instance> ephemeralInstances = new HashSet<>();

    private HealthCheckTask healthCheckTask = new HealthCheckTask(this);
    ;

    /**
     * 加入到心跳线程池
     */
    public void init() {
        GlobalExecutor.scheduleCheckHealth(healthCheckTask);
    }

    public List<Instance> getAllIp() {
        return new ArrayList<>(ephemeralInstances);
    }

    /**
     * 发布service中的信息发生改变事件
     */
    public void pushServiceChanged() {
        PushService pushService = SpringUtils.getBean(PushService.class);
        pushService.serviceChanged(this);
    }

    /**
     * 如果service发生改变，则触发onchange方法
     *
     * @param key
     * @param instances
     */
    @Override
    public void onChange(String key, List<Instance> instances) {

        Set<Instance> instancesSet = new HashSet<>(instances.size());
        instancesSet.addAll(instances);

        ephemeralInstances = instancesSet;

        pushServiceChanged();
    }

    /**
     * 执行刷新客户端心跳任务
     *
     * @param ip
     * @param port
     */
    public void processClientBeat(String ip, String port) {
        RefreshBeatTask task = new RefreshBeatTask(this, ip, port);
        GlobalExecutor.scheduleRefreshHealth(task);
    }
}
