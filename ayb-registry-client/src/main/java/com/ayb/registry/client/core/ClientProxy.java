package com.ayb.registry.client.core;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.core.PushClient;
import com.ayb.registry.common.utils.HttpClientUtils;
import com.ayb.registry.common.utils.JsonUtils;
import com.ayb.registry.common.utils.RestResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端代理
 *
 * @author ayb
 * @date 2023/7/27
 */
@Slf4j
public class ClientProxy {

    public static boolean register(Instance instance, String serverAddress) {
        String url = "http://" + serverAddress + "/instance/canDistro";
        Map<String, String> params = HttpClientUtils.instance2Param(instance);
        try {
            RestResult<String> result = HttpClientUtils.request(url, null, params, null, "POST");
            if (result.ok()) {
                return true;
            }
            log.error("向{}注册实例失败,原因:{}", serverAddress, result.getMessage());
        } catch (Exception e) {
            log.error("向{}注册实例失败,原因:{}", serverAddress, e.getMessage());
        }

        return false;
    }

    public static boolean deregister(Instance instance, String serverAddress) {
        String url = "http://" + serverAddress + "/instance/canDistro";
        Map<String, String> params = HttpClientUtils.instance2Param(instance);
        try {
            RestResult<String> result = HttpClientUtils.request(url, null, params, null, "DELETE");
            if (result.ok()) {
                return true;
            }
            log.error("向{}注册实例失败,原因:{}", serverAddress, result.getMessage());
        } catch (Exception e) {
            log.error("向{}注册实例失败,原因:{}", serverAddress, e.getMessage());
        }

        return false;
    }

    public static List<Instance> getServiceList(String namespaceId, String serviceName, String serverAddress) {
        String url = "http://" + serverAddress + "/instance";
        Map<String, String> params = new HashMap<>(2);
        params.put("namespaceId", namespaceId);
        params.put("serviceName", serviceName);
        try {
            RestResult<String> result = HttpClientUtils.request(url, null, params, null, "GET");
            if (result.ok()) {
                return JsonUtils.toList(result.getData());
            }
            log.error("获取{}列表失败,原因:{}", namespaceId + "##" + serviceName, result.getMessage());
        } catch (Exception e) {
            log.error("获取{}列表失败,原因:{}", namespaceId + "##" + serviceName, e.getMessage());
        }
        return null;
    }

    public static boolean sendHeartbeat(Instance instance, String serverAddress) {
        String url = "http://" + serverAddress + "/instance/canDistro/beat";
        Map<String, String> params = HttpClientUtils.instance2Param(instance);

        try {
            RestResult<String> result = HttpClientUtils.request(url, null, params, null, "PUT");
            if (result.ok()) {
                return true;
            }
            log.error("向{}发送心跳失败,原因:{}", serverAddress, result.getMessage());
        } catch (Exception e) {
            log.error("向{}发送心跳失败,原因:{}", serverAddress, e.getMessage());
        }
        return false;

    }

    public static boolean sendClientInfo(PushClient client, String serverAddress) {
        String url = "http://" + serverAddress + "/instance/canDistro/client";
        Map<String, String> params = HttpClientUtils.client2Param(client);

        try {
            RestResult<String> result = HttpClientUtils.request(url, null, params, null, "PUT");
            if (result.ok()) {
                return true;
            }
            log.error("向{}发送客户端信息失败,原因:{}", serverAddress, result.getMessage());
        } catch (Exception e) {
            log.error("向{}发送客户端信息失败,原因:{}", serverAddress, e.getMessage());
        }
        return false;
    }

    public static int hash(String serviceName) {
        return Math.abs(serviceName.hashCode() % Integer.MAX_VALUE);
    }
}
