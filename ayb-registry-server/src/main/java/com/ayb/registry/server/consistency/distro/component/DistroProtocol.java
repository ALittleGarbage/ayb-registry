package com.ayb.registry.server.consistency.distro.component;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.server.cluster.ServerMember;
import com.ayb.registry.server.cluster.ServerMemberManager;
import com.ayb.registry.server.consistency.distro.DistroDataProcessor;
import com.ayb.registry.server.consistency.distro.DistroDataStorage;
import com.ayb.registry.server.executor.GlobalExecutor;
import com.ayb.registry.server.task.DistroCallback;
import com.ayb.registry.server.task.DistroLoadDataTask;
import com.ayb.registry.server.task.DistroSyncDataTask;
import com.ayb.registry.server.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 用于处理集群之间的操作
 *
 * @author ayb
 * @date 2023/7/21
 */
@Component
public class DistroProtocol {

    @Autowired
    private ServerMemberManager serverMemberManager;

    @Autowired
    private DistroDataProcessor distroDataProcessor;

    @Autowired
    private DistroDataStorage distroDataStorage;

    private DistroSyncDataTask distroSyncDataTask;

    private boolean isInitialized = false;

    /**
     * 初始化，执行数据同步任务，并且载入数据任务
     */
    @PostConstruct
    public void init() {
        distroSyncDataTask = new DistroSyncDataTask(distroDataStorage);
        GlobalExecutor.scheduleClusterSyncData(distroSyncDataTask);

        if (SpringUtils.getStandaloneMode()) {
            this.isInitialized = true;
            return;
        }
        startLoadTask();
    }

    private void startLoadTask() {
        DistroCallback loadCallback = new DistroCallback() {
            @Override
            public void onSuccess() {
                isInitialized = true;
            }

            @Override
            public void onFailed(Throwable throwable) {
                isInitialized = false;
            }
        };
        DistroLoadDataTask distroLoadDataTask = new DistroLoadDataTask(serverMemberManager, loadCallback);

        GlobalExecutor.submitLoadDataTask(distroLoadDataTask);
    }

    public void onReceive(String key, List<Instance> instances) {
        distroDataProcessor.processData(key, instances);
    }

    public byte[] onSnapshot() {
        return distroDataStorage.getDataSnapshot();
    }

    /**
     * 用于将某个service同步到别的集群中
     *
     * @param key
     */
    public void sync(String key) {
        for (ServerMember serverMember : serverMemberManager.allMembersWithoutSelf()) {
            distroSyncDataTask.addTask(key, serverMember.serverAddress());
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
