package com.ayb.registry.server.push;

import com.ayb.registry.server.core.Service;
import org.springframework.context.ApplicationEvent;

/**
 * service更改事件
 *
 * @author ayb
 * @date 2023/7/13
 */
public class ServiceChangeEvent extends ApplicationEvent {

    private Service service;

    public ServiceChangeEvent(Object source, Service service) {
        super(source);
        this.service = service;
    }

    public Service getService() {
        return service;
    }
}
