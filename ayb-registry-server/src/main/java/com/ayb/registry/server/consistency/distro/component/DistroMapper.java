package com.ayb.registry.server.consistency.distro.component;

import com.ayb.registry.server.cluster.ServerMember;
import com.ayb.registry.server.cluster.ServerMembersChangeEvent;
import com.ayb.registry.server.enums.NodeState;
import com.ayb.registry.server.utils.SpringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 存储健康的节点信息，用于路由转发
 *
 * @author ayb
 * @date 2023/7/23
 */
@Component
public class DistroMapper implements ApplicationListener<ServerMembersChangeEvent> {

    private volatile List<String> healthyList = new ArrayList<>();

    /**
     * 当集群节点发生变化时执行，也就是更新健康的节点列表
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ServerMembersChangeEvent event) {
        Set<ServerMember> members = event.getServerMember();

        List<String> serverList = members.stream()
                .filter(member -> member.getState().equals(NodeState.UP))
                .map(ServerMember::serverAddress)
                .sorted()
                .collect(Collectors.toList());

        healthyList = Collections.unmodifiableList(serverList);
    }

    /**
     * 判断是否需要路由转发
     *
     * @param serviceName
     * @return
     */
    public boolean responsible(String serviceName) {
        final List<String> servers = healthyList;

        if (SpringUtils.getStandaloneMode()) {
            return true;
        }

        if (CollectionUtils.isEmpty(servers)) {
            return false;
        }

        InetSocketAddress localAddress = SpringUtils.getLocalAddress();
        int index = servers.indexOf(localAddress.toString());
        int lastIndex = servers.lastIndexOf(localAddress.toString());
        if (lastIndex < 0 || index < 0) {
            return true;
        }

        int target = distroHash(serviceName) % servers.size();
        return target >= index && target <= lastIndex;
    }

    /**
     * 获取特定的节点，对特定的数据操作（取hash值）
     *
     * @param serviceName
     * @return
     */
    public String mapServer(String serviceName) {
        final List<String> servers = this.healthyList;

        if (CollectionUtils.isEmpty(servers)) {
            return SpringUtils.getLocalAddress().toString();
        }

        int index = distroHash(serviceName) % servers.size();
        return servers.get(index);
    }

    /**
     * 用于发布集群信息更改事件
     *
     * @param members
     */
    public void serverMemberChanged(Set<ServerMember> members) {
        SpringUtils.publishEvent(new ServerMembersChangeEvent(this, members));
    }

    private int distroHash(String serviceName) {
        return Math.abs(serviceName.hashCode() % Integer.MAX_VALUE);
    }
}
