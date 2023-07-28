package com.ayb.registry.server.core;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.server.consistency.ConsistencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
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
        Service service = getService(namespaceId, serviceName);
        if (service == null) {
            service = new Service();
            service.setNamespaceId(namespaceId);
            service.setName(serviceName);

            setAndInitService(service);
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
            List<Instance> instances = service.getAllIp();
            instances.remove(instance);
            consistencyService.put(key, instances);
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
            List<Instance> allIp = service.getAllIp();
            // 添加新服务实例
            allIp.add(instance);
            consistencyService.put(key, allIp);
        }
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
