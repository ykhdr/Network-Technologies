package ru.ykhdr.selfcopies.multicast;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.ykhdr.selfcopies.Group;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

@RequiredArgsConstructor
@Component
public class MulticastReceiver extends Thread {
    private final @NotNull MulticastSocket multicastSocket;
    private final @NotNull InetSocketAddress socketAddress;
    private final @NotNull Group group;

    private final byte[] buf = new byte[256];

    @Setter
    @Getter
    private static volatile boolean continueReading = true;

    @Override
    public void run() {
        try {
            multicastSocket.joinGroup(socketAddress, NetworkInterface.getByInetAddress(socketAddress.getAddress()));
            while (continueReading) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(packet);
                byte message = packet.getData()[0];

                System.out.println("Message received: " + message + ". From : " +
                        packet.getAddress().getCanonicalHostName() + ":" + packet.getPort());

                switch (message) {
                    case MulticastPacketMessage.REPORT -> group.addUser(packet.getAddress(), packet.getPort());
                    case MulticastPacketMessage.LEAVE -> group.deleteUser(packet.getAddress(), packet.getPort());
                }

            }

            multicastSocket.leaveGroup(socketAddress, NetworkInterface.getByInetAddress(socketAddress.getAddress()));
            multicastSocket.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
