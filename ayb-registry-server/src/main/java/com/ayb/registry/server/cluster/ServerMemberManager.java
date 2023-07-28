package com.ayb.registry.server.cluster;

import com.ayb.registry.server.consistency.distro.component.DistroMapper;
import com.ayb.registry.server.enums.NodeState;
import com.ayb.registry.server.executor.GlobalExecutor;
import com.ayb.registry.server.lookup.impl.MemoryServerMemberLookup;
import com.ayb.registry.server.task.ServerMemberReportTask;
import com.ayb.registry.server.utils.SpringUtils;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;


/**
 * 管理集群节点
 *
 * @author ayb
 * @date 2023/7/21
 */
@Data
@Component
public class ServerMemberManager implements ApplicationListener<WebServerInitializedEvent> {

    @Autowired
    private DistroMapper distroMapper;

    private volatile ConcurrentSkipListMap<String, ServerMember> serverMap;

    private volatile ServerMember self;

    @PostConstruct
    public void init() {
        this.serverMap = new ConcurrentSkipListMap<>();
        initSelf();

        if (!SpringUtils.getStandaloneMode()) {
            MemoryServerMemberLookup lookup = new MemoryServerMemberLookup(this);
            lookup.start();
        }
    }

    /**
     * 存入自己的节点信息
     */
    private void initSelf() {
        InetSocketAddress localAddress = SpringUtils.getLocalAddress();

        self = new ServerMember();
        self.setIp(localAddress.getHostName());
        self.setPort(localAddress.getPort());

        serverMap.put(self.serverAddress(), self);
    }

    public Set<ServerMember> allMembers() {
        Set<ServerMember> members = new HashSet<>(serverMap.values());
        members.add(self);
        return members;
    }

    public Set<ServerMember> allMembersWithoutSelf() {
        Set<ServerMember> allServer = new HashSet<>(serverMap.values());
        allServer.remove(self);
        return allServer;
    }

    /**
     * web服务初始后，执行集群心跳任务
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        getSelf().setState(NodeState.UP);
        if (!SpringUtils.getStandaloneMode()) {
            ServerMemberReportTask reportTask = new ServerMemberReportTask(this);
            GlobalExecutor.scheduleClusterBeatTask(reportTask);
        }
    }

    /**
     * 用于更新某个节点的信息
     *
     * @param node
     * @return
     */
    public boolean update(ServerMember node) {
        String address = node.serverAddress();
        if (!serverMap.containsKey(address)) {
            return false;
        }

        serverMap.computeIfPresent(address, (s, member) -> {
            if (member.serverAddress().equals(node.serverAddress())) {
                member.copy(node);
                notifyMemberChange();
            }

            return member;
        });

        return true;
    }

    /**
     * 通知集群节点信息更改
     */
    public void notifyMemberChange() {
        distroMapper.serverMemberChanged(allMembers());
    }

    /**
     * 用于载入集群节点的信息
     *
     * @param members
     */
    public synchronized void memberChange(List<ServerMember> members) {
        if (CollectionUtils.isEmpty(members)) {
            return;
        }

        boolean isContainSelf = members.stream()
                .anyMatch(member -> Objects.equals(self.serverAddress(), member.serverAddress()));
        if (!isContainSelf) {
            members.add(this.self);
        }

        boolean hasChange = members.size() != serverMap.size();

        ConcurrentSkipListMap<String, ServerMember> tmpMap = new ConcurrentSkipListMap<>();
        for (ServerMember member : members) {
            String address = member.serverAddress();
            if (!serverMap.containsKey(address)) {
                hasChange = true;
            }
            tmpMap.put(address, member);
        }

        serverMap = tmpMap;

        if (hasChange) {
            notifyMemberChange();
        }
    }
}
