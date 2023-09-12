package ru.ykhdr.selfcopies.multicast;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@AllArgsConstructor
@Component
public class MulticastPublisher {

    private final InetAddress group;
    private final int socketPort;

    public void sendMessage(Byte message) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] buf = {message};
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, socketPort);
            socket.send(packet);
            System.out.println("Successful packet sent : " + message);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void sendReportScheduledTask(){
        sendMessage(MulticastPacketMessage.REPORT);
    }
}
