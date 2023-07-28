package com.ayb.registry.common.udp;

import com.ayb.registry.common.core.Instance;
import com.ayb.registry.common.exception.AybRegistryException;
import com.ayb.registry.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * UDP通讯工具
 *
 * @author ayb
 * @date 2023/7/20
 */
@Slf4j
public class UdpSocket {

    private DatagramSocket udpSocket;

    public UdpSocket() {
        try {
            this.udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            AybRegistryException.cast("创建UdpSocket出错,原因:" + e.getMessage());
        }

    }

    /**
     * 发送更改的service实例信息
     *
     * @param byteData
     * @param address
     * @throws IOException
     */
    public void send(byte[] byteData, InetSocketAddress address) throws IOException {
        DatagramPacket packet = new DatagramPacket(byteData, byteData.length, address);
        udpSocket.send(packet);
    }

    /**
     * 接收更改service的实例信息
     *
     * @return
     * @throws IOException
     */
    public List<Instance> receive() throws IOException {
        byte[] buffer = new byte[1024 * 64];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        udpSocket.receive(packet);

        String json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

        return JsonUtils.toList(json);
    }

    public InetSocketAddress address() {
        String ip = udpSocket.getLocalAddress().getHostAddress();
        int port = udpSocket.getPort();
        return InetSocketAddress.createUnresolved(ip, port);
    }
}
