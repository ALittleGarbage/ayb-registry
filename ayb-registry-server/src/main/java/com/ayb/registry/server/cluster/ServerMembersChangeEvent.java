package com.ayb.registry.server.cluster;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * 用于发布集群节点信息更改事件
 *
 * @author ayb
 * @date 2023/7/24
 */
public class ServerMembersChangeEvent extends ApplicationEvent {

    private Set<ServerMember> members;

    public ServerMembersChangeEvent(Object source, Set<ServerMember> members) {
        super(source);
        this.members = members;
    }

    public Set<ServerMember> getServerMember() {
        return members;
    }
}
