package com.ayb.registry.server.task;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.server.consistency.distro.component.DistroProtocol;
import com.ayb.registry.server.core.Service;
import com.ayb.registry.server.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 刷新实例健康任务
 *
 * @author ayb
 * @date 2023/7/14
 */
@Slf4j
public class RefreshBeatTask implements Runnable {

    private Service service;

    private String ip;

    private String port;

    public RefreshBeatTask(Service service, String ip, String port) {
        this.service = service;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            List<Instance> instances = service.getAllIp();
            boolean isChanged = false;
            for (Instance instance : instances) {
                if (instance.getIp().equals(ip) && instance.getPort().equals(port)) {
                    instance.setLastBeat(System.currentTimeMillis());
                    if (!instance.getHealthy()) {
                        isChanged = true;
                        instance.setHealthy(true);
                    }
                }
            }
            if (isChanged) {
                service.pushServiceChanged();
            }

            // 向集群同步客户端心跳
            DistroProtocol distroProtocol = SpringUtils.getBean(DistroProtocol.class);
            distroProtocol.sync(service.getNamespaceId() + "##" + service.getName());
        } catch (Exception e) {
            log.error("刷新客户端心跳出错,原因:{}", e.getMessage());
        }

    }
}
