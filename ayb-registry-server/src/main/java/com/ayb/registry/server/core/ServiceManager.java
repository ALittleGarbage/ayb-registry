package com.ayb.registry.server.core;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.server.consistency.ConsistencyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * service管理
 *
 * @author ayb
 * @date 2023/7/13
 */
@Slf4j
@Component
public class ServiceManager implements RecordListener<Service> {

    /**
     * Map<namespaceId,Map<serviceName,Service>>  内存注册表
     */
    private final Map<String, Map<String, Service>> serviceMap = new ConcurrentHashMap<>();

    @Autowired
    private ConsistencyService consistencyService;

    private final Object putServiceLock = new Object();

    /**
     * 将此ServiceManager添加监听
     */
    @PostConstruct
    public void init() {
        consistencyService.listen("ServiceManager", this);
    }

    /**
     * 初始化一个空的service
     *
     * @param namespaceId
     * @param serviceName
     */
    public void initEmptyService(String namespaceId, String serviceName) {
        if (!containService(namespaceId, serviceName)) {
            synchronized (putServiceLock) {
                if (containService(namespaceId, serviceName)) {
                    return;
                }
                Service service = new Service();
                service.setNamespaceId(namespaceId);
                service.setName(serviceName);

                setAndInitService(service);
            }
        }
    }

    /**
     * 登记实例
     *
     * @param instance
     */
    public void registerInstance(Instance instance) {
        initEmptyService(instance.getNamespaceId(), instance.getServiceName());

        addInstance(instance);
    }

    /**
     * 注销实例
     *
     * @param instance
     */
    public void deregisterInstance(Instance instance) {
        Service service = getService(instance.getNamespaceId(), instance.getServiceName());
        if (service == null) {
            return;
        }

        String key = instance.getNamespaceId() + "##" + instance.getServiceName();

        synchronized (service) {
            Set<Instance> instanceSet = getInstanceSet(key, service, instance);

            instanceSet.remove(instance);

            consistencyService.put(key, new ArrayList<>(instanceSet));
        }
    }

    /**
     * 通过命名空间和serviceName获取对应的service
     *
     * @param namespaceId
     * @param serviceName
     * @return
     */
    public Service getService(String namespaceId, String serviceName) {
        Map<String, Service> namespaceIdMap = serviceMap.get(namespaceId);
        if (namespaceIdMap == null) {
            return null;
        }
        return namespaceIdMap.get(serviceName);
    }

    /**
     * 命名空间和serviceName获取对应的service是否已经被创建
     *
     * @param namespaceId
     * @param serviceName
     * @return
     */
    public boolean containService(String namespaceId, String serviceName) {
        return getService(namespaceId, serviceName) != null;
    }

    /**
     * 将创建好的service放入serviceMap
     *
     * @param service
     */
    public void setService(Service service) {
        // 如果不存在，则初始化service
        if (!serviceMap.containsKey(service.getNamespaceId())) {
            synchronized (this) {
                if (!serviceMap.containsKey(service.getNamespaceId())) {
                    serviceMap.put(service.getNamespaceId(), new ConcurrentSkipListMap<>());
                }
            }
        }
        serviceMap.get(service.getNamespaceId()).put(service.getName(), service);
    }

    private void addInstance(Instance instance) {
        String key = instance.getNamespaceId() + "##" + instance.getServiceName();

        Service service = getService(instance.getNamespaceId(), instance.getServiceName());

        synchronized (service) {
            Set<Instance> instanceSet = getInstanceSet(key, service, instance);

            instanceSet.add(instance);

            consistencyService.put(key, new ArrayList<>(instanceSet));
        }
    }

    private Set<Instance> getInstanceSet(String key, Service service, Instance instance) {
        List<Instance> instances = consistencyService.get(key);
        if(CollectionUtils.isEmpty(instances)) {
            return new HashSet<>();
        }

        List<Instance> allIp = service.getAllIp();
        Map<String, Instance> ipMap = new HashMap<>(allIp.size());
        for (Instance ip : allIp) {
            ipMap.put(ip.address(), ip);
        }

        Set<Instance> instanceSet = new HashSet<>(instances.size());
        for (Instance in : instances) {
            Instance ip = ipMap.get(instance.address());
            if (ip != null) {
                in.setHealthy(ip.getHealthy());
                in.setLastBeat(ip.getLastBeat());
            }
            instanceSet.add(in);
        }
        return instanceSet;
    }

    @Override
    public void onChange(String key, Service service) {
        if (service == null) {
            log.warn("received empty push from raft, key: {}", key);
            return;
        }

        Service oldService = getService(service.getNamespaceId(), service.getName());
        if (oldService != null) {
            consistencyService.listen(key, oldService);
        } else {
            setAndInitService(service);
        }
    }

    private void setAndInitService(Service service) {
        setService(service);
        service.init();
        consistencyService.listen(service.getNamespaceId() + "##" + service.getName(), service);
    }
}
