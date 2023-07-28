package com.ayb.registry.server.cluster;

import com.ayb.registry.server.enums.NodeState;
import lombok.Data;

import java.util.Map;
import java.util.Objects;

/**
 * 集群节点信息
 *
 * @author ayb
 * @date 2023/7/21
 */
@Data
public class ServerMember {

    Map<String, String> meta;
    private String ip;
    private Integer port;
    private NodeState state;

    public String serverAddress() {
        return ip + ":" + port;
    }

    public void copy(ServerMember node) {
        this.ip = node.getIp();
        this.port = node.getPort();
        this.state = node.getState();
        this.meta = node.getMeta();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerMember that = (ServerMember) o;
        return Objects.equals(serverAddress(), that.serverAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverAddress());
    }
}
