package com.ayb.registry.server.web;

import com.ayb.registry.common.utils.HttpClientUtils;
import com.ayb.registry.common.utils.JsonUtils;
import com.ayb.registry.common.utils.RestResult;
import com.ayb.registry.server.consistency.distro.component.DistroMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 路由转发过滤器
 *
 * @author ayb
 * @date 2023/7/22
 */
@Slf4j
@Component
public class DistroFilter implements Filter {

    @Autowired
    private DistroMapper distroMapper;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            String path = new URI(request.getRequestURI()).getPath();
            String serviceName = request.getParameter("serviceName");

            // 判断是否需要转发
            if (path.contains("/instance/canDistro") && !distroMapper.responsible(serviceName)) {
                // 获取到特定的节点地址
                final String targetServer = distroMapper.mapServer(serviceName);

                log.info("需要进行路由转发，url:{},转发地址:{}", path, targetServer);

                final Map<String, String> headers = getHeaders(request);
                final Map<String, String> params = getParam(request.getParameterMap());
                final String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
                String url = "http://" + targetServer + request.getRequestURI();

                RestResult<String> result = HttpClientUtils.request(url, headers, params, body, request.getMethod());

                resp(response, result);
            } else {
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            log.error("路由请求转发时发生错误,原因:{}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "路由请求转发时发生错误,原因:" + e.getMessage());
        }
    }

    private Map<String, String> getParam(Map<String, String[]> parameterMap) {
        Map<String, String> map = new HashMap<>(16);
        for (String key : parameterMap.keySet()) {
            map.put(key, parameterMap.get(key)[0]);
        }
        return map;
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headerMap = new HashMap<>(16);
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headerName = headers.nextElement();
            headerMap.put(headerName, request.getHeader(headerName));
        }
        return headerMap;
    }

    private void resp(HttpServletResponse response, RestResult<String> result) throws Exception {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.toJson(result));
        response.setStatus(result.getCode());
    }
}
