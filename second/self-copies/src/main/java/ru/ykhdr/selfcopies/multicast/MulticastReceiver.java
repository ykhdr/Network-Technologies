package ru.ykhdr.selfcopies.multicast;

import ru.ykhdr.selfcopies.Group;

import java.io.IOException;
import java.net.*;

public class MulticastReceiver extends Thread {
    private final MulticastSocket socket;
    private final InetSocketAddress socketAddress;
    private final NetworkInterface networkInterface;
    private final MulticastPublisher publisher;
    private final byte[] buf = new byte[256];

    public MulticastReceiver(InetAddress group, MulticastPublisher publisher) {
        this.publisher = publisher;
        try {
            socket = new MulticastSocket(MulticastConfig.PORT);
            socketAddress = new InetSocketAddress(group, 8080);
            networkInterface = NetworkInterface.getByInetAddress(group);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            socket.joinGroup(socketAddress, networkInterface);
            System.out.println("Successful joined group");
            while (MulticastConfig.continueReading) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                System.out.println("Message recieved: " + message + ".\n From : " + packet.getAddress());

                switch (message) {
                    case MulticastPacketMessage.JOIN -> {
                        Group.getInstance().addAddress(packet.getAddress());
                        publisher.sendMessage(MulticastPacketMessage.HELLO);
                    }
                    case MulticastPacketMessage.EXIT -> Group.getInstance().deleteAddress(packet.getAddress());
                    case MulticastPacketMessage.HELLO -> Group.getInstance().addAddress(packet.getAddress());

                    default -> {
                    }
                }

            }

            socket.leaveGroup(socketAddress, networkInterface);
            socket.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
