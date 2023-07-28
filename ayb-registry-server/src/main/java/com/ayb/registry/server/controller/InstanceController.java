package com.ayb.registry.server.controller;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.core.PushClient;
import com.ayb.registry.common.exception.AybRegistryException;
import com.ayb.registry.common.utils.JsonUtils;
import com.ayb.registry.common.utils.RestResult;
import com.ayb.registry.server.core.Service;
import com.ayb.registry.server.core.ServiceManager;
import com.ayb.registry.server.push.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * @author ayb
 * @date 2023/7/12
 */
@Slf4j
@RestController
@RequestMapping("/instance")
public class InstanceController {

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private PushService pushService;

    /**
     * 实例登记
     *
     * @param instance
     * @return
     */
    @PostMapping("/canDistro")
    public RestResult<String> register(Instance instance) {
        instance.setLastBeat(System.currentTimeMillis());
        instance.setHealthy(true);
        serviceManager.registerInstance(instance);
        return RestResult.<String>builder()
                .withCode(HttpServletResponse.SC_OK)
                .withData("ok")
                .build();
    }

    /**
     * 实例注销
     *
     * @param instance
     * @return
     */
    @DeleteMapping("/canDistro")
    public RestResult<String> deregister(Instance instance) {
        serviceManager.deregisterInstance(instance);
        return RestResult.<String>builder()
                .withCode(HttpServletResponse.SC_OK)
                .withData("ok")
                .build();
    }

    /**
     * 获取service列表
     *
     * @param namespaceId
     * @param serviceName
     * @return
     */
    @GetMapping
    public RestResult<String> getServiceList(
            @RequestParam("namespaceId") String namespaceId,
            @RequestParam("serviceName") String serviceName) {
        Service service = serviceManager.getService(namespaceId, serviceName);
        if (service == null) {
            AybRegistryException.cast(serviceName + "没有找到");
        }

        return RestResult.<String>builder()
                .withCode(HttpServletResponse.SC_OK)
                .withData(JsonUtils.toJson(service.getAllIp()))
                .build();
    }

    /**
     * 接收实例心跳
     *
     * @param instance
     * @return
     */
    @PutMapping("/canDistro/beat")
    public RestResult<String> heartbeat(Instance instance) {
        Service service = serviceManager.getService(instance.getNamespaceId(), instance.getServiceName());
        if (service == null) {
            AybRegistryException.cast("服务未找到: " + instance.getNamespaceId() + "@" + instance.getServiceName());
        }

        service.processClientBeat(instance.getIp(), instance.getPort());

        return RestResult.<String>builder()
                .withCode(HttpServletResponse.SC_OK)
                .withData("ok")
                .build();
    }

    /**
     * 接收客户端心跳
     *
     * @param pushClient
     * @return
     */
    @PutMapping("/canDistro/client")
    public RestResult<String> registerOrRefreshClient(PushClient pushClient) {
        pushService.addOrRefreshClient(pushClient);
        return RestResult.<String>builder()
                .withCode(HttpServletResponse.SC_OK)
                .withData("ok")
                .build();
    }
}
