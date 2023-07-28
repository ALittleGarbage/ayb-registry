package com.ayb.registry.server.lookup.impl;

import com.ayb.registry.server.cluster.ServerMember;
import com.ayb.registry.server.cluster.ServerMemberManager;
import com.ayb.registry.server.enums.NodeState;
import com.ayb.registry.server.lookup.AbstractServerMemberLookup;

import java.util.ArrayList;

/**
 * 载入内存中的集群节点地址
 *
 * @author ayb
 * @date 2023/7/24
 */
public class MemoryServerMemberLookup extends AbstractServerMemberLookup {

    public MemoryServerMemberLookup(ServerMemberManager serverMemberManager) {
        super(serverMemberManager);
    }

    @Override
    public void start() {
        ArrayList<ServerMember> members = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            ServerMember serverMember = new ServerMember();
            serverMember.setState(NodeState.UP);
            serverMember.setIp("192.168.43.6");
            serverMember.setPort(8888 + i);
            members.add(serverMember);
        }

        afterLookup(members);
    }
}
