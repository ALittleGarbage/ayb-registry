package com.ayb.registry.common.utils;


import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.core.PushClient;
import com.ayb.registry.common.exception.AybRegistryException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Http工具
 *
 * @author ayb
 * @date 2023/7/21
 */
@Slf4j
public class HttpClientUtils {

    private static CloseableHttpAsyncClient asyncHttpClient;

    private static CloseableHttpClient syncHttpClient;

    static {
        syncHttpClient = HttpClients.createDefault();

        asyncHttpClient = HttpAsyncClients.createDefault();
        asyncHttpClient.start();
    }

    public static RestResult<String> httpPut(String url, Map<String, String> headers, byte[] content) throws Exception {

        CloseableHttpResponse response = null;
        try {
            HttpPut httpPut = new HttpPut(url);

            setHeader(headers, httpPut);

            setBody(content, httpPut);

            response = syncHttpClient.execute(httpPut);

            String json = EntityUtils.toString(response.getEntity());

            return JsonUtils.toObj(json, RestResult.class);
        } catch (Exception e) {
            log.error("发送同步数据请求失败,原因:{}", e.getMessage());

            return RestResult.<String>builder()
                    .withCode(500)
                    .withMsg(e.getMessage())
                    .build();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static RestResult<String> httpGet(String url, Map<String, String> param) throws Exception {

        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(getUri(url, param));

            response = syncHttpClient.execute(httpGet);

            String json = EntityUtils.toString(response.getEntity());

            return JsonUtils.toObj(json, RestResult.class);
        } catch (Exception e) {
            log.error("获取同步数据失败,原因：{}", e.getMessage());
            return RestResult.<String>builder()
                    .withCode(500)
                    .withMsg(e.getMessage())
                    .build();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static RestResult<String> request(String url, Map<String, String> headers,
                                             Map<String, String> params, String body, String method) throws Exception {

        CloseableHttpResponse response = null;
        URI uri = getUri(url, params);
        try {
            HttpEntityEnclosingRequestBase httpRequest = getRequest(method);
            httpRequest.setURI(uri);
            setHeader(headers, httpRequest);
            setBody(body, httpRequest);

            response = syncHttpClient.execute(httpRequest);

            String json = EntityUtils.toString(response.getEntity());

            return JsonUtils.toObj(json, RestResult.class);
        } catch (Exception e) {
            log.error("{}请求失败,原因:{}", uri.toString(), e.getMessage());
            return RestResult.<String>builder()
                    .withCode(500)
                    .withMsg(e.getMessage())
                    .build();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static void asyncRequest(String url, Map<String, String> headers,
                                    Map<String, String> params, String body, String method,
                                    FutureCallback<HttpResponse> callback) {
        HttpEntityEnclosingRequestBase request = getRequest(method);
        request.setURI(getUri(url, params));
        setHeader(headers, request);
        setBody(body, request);

        asyncHttpClient.execute(request, callback);
    }

    public static void asyncHttpPost(String url, byte[] content, FutureCallback<HttpResponse> callback) {
        asyncRequest(url, null, null, new String(content), "POST", callback);
    }

    public static void asyncHttpDelete(String url, Map<String, String> params, FutureCallback<HttpResponse> callback) {
        asyncRequest(url, null, params, null, "DELETE", callback);
    }

    public static Map<String, String> instance2Param(Instance instance) {
        HashMap<String, String> params = new HashMap<>(16);
        params.put("namespaceId", instance.getNamespaceId());
        params.put("serviceName", instance.getServiceName());
        params.put("ip", instance.getIp());
        params.put("port", instance.getPort());
        return params;
    }

    public static Map<String, String> client2Param(PushClient client) {
        HashMap<String, String> params = new HashMap<>(16);
        params.put("namespaceId", client.getNamespaceId());
        params.put("serviceName", client.getServiceName());
        params.put("ip", client.getIp());
        params.put("port", String.valueOf(client.getPort()));
        return params;
    }

    public static void shutdownAll() {
        try {
            if (syncHttpClient != null) {
                syncHttpClient.close();
            }
            if (asyncHttpClient != null && asyncHttpClient.isRunning()) {
                asyncHttpClient.close();
            }
        } catch (Exception e) {
            log.error("关闭HttpClient出错,原因:{}", e.getMessage());
        }
    }

    private static HttpEntityEnclosingRequestBase getRequest(String method) {
        if (method == null) {
            AybRegistryException.cast("请求方法为空或者错误");
        }

        return new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return method;
            }
        };
    }

    private static void setHeader(Map<String, String> headers, HttpRequestBase http) {
        if (MapUtils.isEmpty(headers)) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if ("content-length".equals(entry.getKey())) {
                continue;
            }
            http.setHeader(entry.getKey(), entry.getValue());
        }
    }

    private static URI getUri(String url, Map<String, String> param) {
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            if (!MapUtils.isEmpty(param)) {
                for (String key : param.keySet()) {
                    uriBuilder.addParameter(key, param.get(key));
                }
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void setBody(byte[] content, HttpEntityEnclosingRequestBase http) {
        if (content.length == 0) {
            return;
        }
        setBody(new String(content, StandardCharsets.UTF_8), http);
    }

    private static void setBody(String json, HttpEntityEnclosingRequestBase http) {
        if (StringUtils.isEmpty(json)) {
            return;
        }
        http.setEntity(new StringEntity(json, ContentType.create("application/json", StandardCharsets.UTF_8)));
    }
}
