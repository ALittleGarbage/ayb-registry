package com.ayb.registry.server.task;

import com.ayb.registry.common.utils.JsonUtils;
import com.ayb.registry.common.utils.RestResult;
import com.ayb.registry.server.cluster.ServerMember;
import com.ayb.registry.server.cluster.ServerMemberManager;
import com.ayb.registry.server.consistency.distro.component.DistroHttpClient;
import com.ayb.registry.server.enums.NodeState;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.util.EntityUtils;

/**
 * 用于本地节点向集群发送心跳
 *
 * @author ayb
 * @date 2023/7/23
 */
@Slf4j
public class ServerMemberReportTask implements Runnable {

    private ServerMemberManager serverMemberManager;

    public ServerMemberReportTask(ServerMemberManager serverMemberManager) {
        this.serverMemberManager = serverMemberManager;
    }

    @Override
    public void run() {
        try {
            for (ServerMember member : serverMemberManager.allMembersWithoutSelf()) {
                DistroHttpClient.asyncSendBeat(member, new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse httpResponse) {
                        try {
                            String json = EntityUtils.toString(httpResponse.getEntity());
                            RestResult response = JsonUtils.toObj(json, RestResult.class);
                            if (response.ok()) {
                                onSuccess(member);
                            }
                        } catch (Exception e) {
                            log.error("向{}地址发送心跳失败,原因:{}", member.serverAddress(), e.getMessage());
                            onFail(member);
                        }
                    }

                    @Override
                    public void failed(Exception e) {
                        log.error("向{}地址发送心跳失败,原因:{}", member.serverAddress(), e.getMessage());
                        onFail(member);
                    }

                    @Override
                    public void cancelled() {

                    }
                });
            }
        } catch (Exception e) {
            log.error("发送心跳失败,原因:{}", e.getMessage());
        }

    }

    private void onSuccess(ServerMember member) {
        if (!member.getState().equals(NodeState.UP)) {
            member.setState(NodeState.UP);
            serverMemberManager.notifyMemberChange();
        }
    }

    private void onFail(ServerMember member) {
        if (!member.getState().equals(NodeState.DOWN)) {
            member.setState(NodeState.DOWN);
            serverMemberManager.notifyMemberChange();
        }
    }
}
