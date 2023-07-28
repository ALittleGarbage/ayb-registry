package com.ayb.registry.server.consistency.distro.component;

import com.ayb.registry.common.exception.AybRegistryException;
import com.ayb.registry.common.utils.HttpClientUtils;
import com.ayb.registry.common.utils.JsonUtils;
import com.ayb.registry.common.utils.RestResult;
import com.ayb.registry.server.cluster.ServerMember;
import com.ayb.registry.server.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于集群间通信的工具
 *
 * @author ayb
 * @date 2023/7/22
 */
@Slf4j
@Component
public class DistroHttpClient {

    static {
        // 注册HttpClient销毁方法
        SpringUtils.shutdownHook(new Thread(HttpClientUtils::shutdownAll));
    }

    /**
     * 将更改的数据同步到别的集群节点中
     *
     * @param data
     * @param serverAddress
     */
    public static void syncData(byte[] data, String serverAddress) {
        Map<String, String> headers = new HashMap<>(128);
        headers.put("Accept-Encoding", "gzip,deflate,sdch");
        headers.put("Connection", "Keep-Alive");
        headers.put("Content-Encoding", "gzip");

        String url = "http://" + serverAddress + "/distro/sync";
        try {
            RestResult<String> result = HttpClientUtils.httpPut(url, headers, data);
            if (!result.ok()) {
                AybRegistryException.cast("发送同步数据请求失败,发送地址:" + serverAddress + ",原因:" + result.getMessage());
            }
        } catch (Exception e) {
            log.error("同步数据时发生错误,原因:{}", e.getMessage());
        }
    }

    /**
     * 异步发送集群心跳
     *
     * @param member
     * @param callback
     */
    public static void asyncSendBeat(ServerMember member, FutureCallback<HttpResponse> callback) {
        String url = "http://" + member.serverAddress() + "/cluster/report";
        String json = JsonUtils.toJson(member);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        HttpClientUtils.asyncHttpPost(url, data, callback);
    }

    /**
     * 从别的集群节点获取数据，用于节点初始化载入数据
     *
     * @param serverAddress
     * @return
     */
    public static byte[] getDataSnapshot(String serverAddress) {
        String url = "http://" + serverAddress + "/distro/snapshot";
        try {
            RestResult<String> response = HttpClientUtils.httpGet(url, null);
            if (response.ok()) {
                if (StringUtils.isEmpty(response.getData())) {
                    log.info("从{}地址获取同步数据为空", serverAddress);
                    return new byte[0];
                }
                return response.getData().getBytes(StandardCharsets.UTF_8);
            }
            return null;
        } catch (Exception e) {
            AybRegistryException.cast("从" + serverAddress + "地址获取同步数据失败,原因:" + e.getMessage());
        }
        return null;
    }
}
