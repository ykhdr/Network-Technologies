package ru.ykhdr.selfcopies.multicast;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


@Component
public class MulticastPublisher {

    private final InetAddress group;
    private final int socketPort;
    private final DatagramSocket socket;

    public MulticastPublisher(InetAddress group, int socketPort) {
        this.group = group;
        this.socketPort = socketPort;
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(Byte message) {
        try {
            byte[] buf = {message};
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, socketPort);
            socket.send(packet);
            System.out.println("Successful packet sent : " + message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeSocket(){
        socket.close();
    }

    @Scheduled(fixedDelay = 5000)
    public void sendReportScheduledTask(){
        sendMessage(MulticastPacketMessage.REPORT);
    }
}
