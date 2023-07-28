package com.ayb.registry.server.task;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.utils.HttpClientUtils;
import com.ayb.registry.common.utils.JsonUtils;
import com.ayb.registry.common.utils.RestResult;
import com.ayb.registry.server.core.Service;
import com.ayb.registry.server.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * 实例健康检查任务
 *
 * @author ayb
 * @date 2023/7/14
 */
@Slf4j
public class HealthCheckTask implements Runnable {

    private Service service;

    public HealthCheckTask(Service service) {
        this.service = service;
    }

    @Override
    public void run() {
        try {
            boolean isChanged = false;
            List<Instance> instances = service.getAllIp();
            // 设置不健康实例
            for (Instance instance : instances) {
                if (System.currentTimeMillis() - instance.getLastBeat() > 15 * 1000) {
                    if (instance.getHealthy()) {
                        isChanged = true;
                        instance.setHealthy(false);
                    }
                }
            }
            // 删除过期实例
            for (Instance instance : instances) {
                if (System.currentTimeMillis() - instance.getLastBeat() > 30 * 1000) {
                    isChanged = true;
                    deleteIp(instance);
                }
            }
            if (isChanged) {
                service.pushServiceChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("检查实例健康时出现错误,原因:{}", e.getMessage());
        }
    }

    /**
     * 删除过期的实例
     * 为为什么要用http请求来删除实例呢？
     * 因为根据distro协议，在写操作中特定的数据要用特定的节点操作
     * 有可能当前是节点不是那个特定的节点，需要使用http请求经过路由转发到指定的节点中。
     * 如果当前结点删除实例，那么也要将数据同步到集群中，这就造成了数据冗余和一个数据同步问题
     *
     * @param instance
     */
    private void deleteIp(Instance instance) {
        InetSocketAddress localAddress = SpringUtils.getLocalAddress();

        String url = "http://" + localAddress.toString() + "/instance/canDistro";
        Map<String, String> params = HttpClientUtils.instance2Param(instance);

        HttpClientUtils.asyncHttpDelete(url, params, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                try {
                    String json = EntityUtils.toString(httpResponse.getEntity());
                    RestResult result = JsonUtils.toObj(json, RestResult.class);
                    if (!result.ok()) {
                        log.error("删除实例{}失败", instance);
                    }
                } catch (IOException e) {
                    log.error("删除实例{}失败,原因:{}", instance, e.getMessage());
                }

            }

            @Override
            public void failed(Exception e) {
                log.error("删除实例{}失败,原因:{}", instance, e.getMessage());
            }

            @Override
            public void cancelled() {

            }
        });
    }
}
