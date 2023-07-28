package com.ayb.registry.server.lookup;

import com.ayb.registry.server.cluster.ServerMember;
import com.ayb.registry.server.cluster.ServerMemberManager;

import java.util.List;

/**
 * @author ayb
 * @date 2023/7/23
 */
public abstract class AbstractServerMemberLookup implements ServerMemberLookup {

    private ServerMemberManager serverMemberManager;

    public AbstractServerMemberLookup(ServerMemberManager serverMemberManager) {
        this.serverMemberManager = serverMemberManager;
    }

    public void afterLookup(List<ServerMember> members) {
        serverMemberManager.memberChange(members);
    }
}
