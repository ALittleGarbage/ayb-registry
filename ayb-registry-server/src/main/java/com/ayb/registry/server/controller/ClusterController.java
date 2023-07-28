package com.ayb.registry.server.controller;

import com.ayb.registry.common.utils.RestResult;
import com.ayb.registry.server.cluster.ServerMember;
import com.ayb.registry.server.cluster.ServerMemberManager;
import com.ayb.registry.server.enums.NodeState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @author ayb
 * @date 2023/7/23
 */
@RestController
@RequestMapping("/cluster")
public class ClusterController {

    @Autowired
    private ServerMemberManager serverMemberManager;

    /**
     * 用于接收集群的心跳
     *
     * @param node
     * @return
     */
    @PostMapping("/report")
    public RestResult<String> report(@RequestBody ServerMember node) {
        if (node == null) {
            return RestResult.<String>builder()
                    .withCode(HttpServletResponse.SC_BAD_REQUEST)
                    .withMsg("节点数据信息为空")
                    .build();
        }
        node.setState(NodeState.UP);
        boolean result = serverMemberManager.update(node);
        return RestResult.<String>builder()
                .withCode(HttpServletResponse.SC_OK)
                .withData(Boolean.toString(result))
                .build();
    }

}
