package com.ayb.registry.server.task;

/**
 * 回调函数
 *
 * @author ayb
 * @date 2023/7/22
 */
public interface DistroCallback {

    /**
     * 成功执行的方法
     */
    void onSuccess();

    /**
     * 失败执行的方法
     *
     * @param throwable
     */
    void onFailed(Throwable throwable);
}
