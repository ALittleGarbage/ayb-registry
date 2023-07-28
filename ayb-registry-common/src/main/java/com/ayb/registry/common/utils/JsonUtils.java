package com.ayb.registry.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ayb.registry.common.core.Instance;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * json格式化工具
 *
 * @author ayb
 * @date 2023/7/20
 */
public class JsonUtils {

    public static String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }

    public static <T> T toObj(String json, Class<T> type) {
        return JSON.parseObject(json, type);
    }

    public static <T> List<T> toList(String json) {
        return JSON.parseObject(json, List.class);
    }

    public static Map<String, List<Instance>> toMap(String json) {
        return JSON.parseObject(json, new TypeReference<Map<String, List<Instance>>>() {
        });
    }

    public static <T> byte[] toJsonByte(T data) {
        return JSON.toJSONString(data).getBytes(StandardCharsets.UTF_8);
    }

    public static <T> T toObj(byte[] data, Class<T> type) {
        return JSON.parseObject(new String(data), type);
    }
}
