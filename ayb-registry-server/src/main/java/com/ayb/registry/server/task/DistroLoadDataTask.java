package com.ayb.registry.server.task;

import com.ayb.registry.server.cluster.ServerMember;
import com.ayb.registry.server.cluster.ServerMemberManager;
import com.ayb.registry.server.consistency.distro.DistroDataProcessor;
import com.ayb.registry.server.consistency.distro.component.DistroHttpClient;
import com.ayb.registry.server.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 初始化载入数据任务
 *
 * @author ayb
 * @date 2023/7/22
 */
@Slf4j
public class DistroLoadDataTask implements Runnable {

    private DistroDataProcessor distroDataProcessor;

    private ServerMemberManager serverMemberManager;

    private DistroCallback loadDataCallback;

    public DistroLoadDataTask(ServerMemberManager serverMemberManager, DistroCallback loadDataCallback) {
        this.distroDataProcessor = SpringUtils.getBean(DistroDataProcessor.class);
        this.serverMemberManager = serverMemberManager;
        this.loadDataCallback = loadDataCallback;
    }

    @Override
    public void run() {
        try {
            load();
            loadDataCallback.onSuccess();
        } catch (Exception e) {
            loadDataCallback.onFailed(e);
        }

    }

    /**
     * 从别的集群获取数据快照并载入
     *
     * @throws Exception
     */
    private void load() throws Exception {
        while (serverMemberManager.allMembersWithoutSelf().isEmpty()) {
            TimeUnit.SECONDS.sleep(1);
        }

        for (ServerMember each : serverMemberManager.allMembersWithoutSelf()) {
            try {
                log.info("从{}地址,请求同步数据", each.serverAddress());
                // 请求获取数据快照
                byte[] data = DistroHttpClient.getDataSnapshot(each.serverAddress());
                if (data == null) {
                    continue;
                }
                if (data.length == 0) {
                    return;
                }
                // 载入数据
                boolean result = distroDataProcessor.processSnapshot(data);
                if (result) {
                    return;
                }
            } catch (Exception e) {
                log.error("从{}地址,初始化载入远程数据失败,原因:{}", each.serverAddress(), e.getMessage());
            }
        }
    }
}
