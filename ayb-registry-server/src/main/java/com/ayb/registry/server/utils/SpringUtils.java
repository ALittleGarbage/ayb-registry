package com.ayb.registry.server.utils;

import com.ayb.registry.common.exception.AybRegistryException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * spring工具
 *
 * @author ayb
 * @date 2023/7/12
 */
public class SpringUtils implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static ApplicationContext context;

    public static ApplicationContext getContext() {
        return context;
    }

    public static InetSocketAddress getLocalAddress() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String port = env().getProperty("server.port");
            return InetSocketAddress.createUnresolved(ip, Integer.parseInt(port));
        } catch (Exception e) {
            AybRegistryException.cast("获取本地地址失败,原因:" + e.getMessage());
        }
        return null;
    }

    public static boolean isStandaloneMode() {
        //TODO true单机 false集群
        return true;
    }

    public static <T> T getBean(Class<T> beanType) {
        return context.getBean(beanType);
    }

    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    public static <T> T getBean(String beanName, Class<T> beanType) {
        return context.getBean(beanName, beanType);
    }

    public static void publishEvent(ApplicationEvent event) {
        context.publishEvent(event);
    }

    public static void shutdownHook(Thread thread) {
        Runtime.getRuntime().addShutdownHook(thread);
    }

    private static Environment env() {
        return context.getEnvironment();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        context = applicationContext;
    }
}
