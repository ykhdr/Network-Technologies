package ru.ykhdr.selfcopies.multicast;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.ykhdr.selfcopies.Group;
import ru.ykhdr.selfcopies.config.MulticastConfig;

import java.io.IOException;
import java.net.*;

@AllArgsConstructor
@Component
public class MulticastReceiver extends Thread {
    private final @NotNull MulticastSocket multicastSocket;
    private final @NotNull InetSocketAddress socketAddress;
    private final @NotNull NetworkInterface networkInterface;
    private final @NotNull MulticastPublisher publisher;
    private final @NotNull Group group;
    private final byte[] buf = new byte[256];


    public static volatile boolean continueReading = true;

    @Override
    public void run() {
        try {
            multicastSocket.joinGroup(socketAddress, networkInterface);
            while (continueReading) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                System.out.println("Message received: " + message + ". From : " + packet.getAddress());

                switch (Byte.parseByte(message)) {
                    case MulticastPacketMessage.REPORT -> group.addAddress(packet.getAddress());
                    case MulticastPacketMessage.LEAVE -> group.deleteAddress(packet.getAddress());
                }

            }

            multicastSocket.leaveGroup(socketAddress, networkInterface);
            multicastSocket.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
