package ru.ykhdr.selfcopies.multicast;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@AllArgsConstructor
public class MulticastPublisher {

    private final InetAddress group;

    public void sendMessage(String message) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] buf = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MulticastConfig.PORT);
            socket.send(packet);
            System.out.println("Successful packet sent : " + message);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
