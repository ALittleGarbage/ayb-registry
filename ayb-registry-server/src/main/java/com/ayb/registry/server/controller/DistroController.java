package com.ayb.registry.server.controller;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.exception.AybRegistryException;
import com.ayb.registry.common.utils.RestResult;
import com.ayb.registry.server.consistency.distro.component.DistroProtocol;
import com.ayb.registry.server.core.ServiceManager;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author ayb
 * @date 2023/7/21
 */
@RestController
@RequestMapping("/distro")
public class DistroController {

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private DistroProtocol distroProtocol;

    /**
     * 接收同步数据
     *
     * @param dataMap
     * @return
     */
    @PutMapping("/sync")
    public RestResult<String> onSyncData(@RequestBody Map<String, List<Instance>> dataMap) {
        if (MapUtils.isEmpty(dataMap)) {
            AybRegistryException.cast("同步数据为空");
        }

        for (Map.Entry<String, List<Instance>> entry : dataMap.entrySet()) {
            String[] split = entry.getKey().split("##");
            String namespaceId = split[0];
            String serviceName = split[1];
            if (!serviceManager.containService(namespaceId, serviceName)) {
                serviceManager.initEmptyService(namespaceId, serviceName);
            }
            distroProtocol.onReceive(entry.getKey(), entry.getValue());
        }

        return RestResult.<String>builder()
                .withCode(HttpServletResponse.SC_OK)
                .withData("ok")
                .build();
    }

    /**
     * 获取所有的同步数据快照
     *
     * @return
     */
    @GetMapping("/snapshot")
    public RestResult<byte[]> getDataSnapshot() {
        return RestResult.<byte[]>builder()
                .withCode(HttpServletResponse.SC_OK)
                .withData(distroProtocol.onSnapshot())
                .build();
    }
}
