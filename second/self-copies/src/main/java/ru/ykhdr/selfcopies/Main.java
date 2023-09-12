package ru.ykhdr.selfcopies;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.ykhdr.selfcopies.config.MulticastConfig;
import ru.ykhdr.selfcopies.config.SpringConfig;
import ru.ykhdr.selfcopies.multicast.MulticastPacketMessage;
import ru.ykhdr.selfcopies.multicast.MulticastPublisher;
import ru.ykhdr.selfcopies.multicast.MulticastReceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Optional;


public class Main {
    public static void main(String[] args) throws UnknownHostException {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfig.class);

        Group group = ctx.getBean(Group.class);

        MulticastPublisher publisher = ctx.getBean(MulticastPublisher.class);
//        MulticastReceiver receiver = new MulticastReceiver(
//                (MulticastSocket) ctx.getBean("multicastSocket"),
//                (InetSocketAddress) ctx.getBean("socketAddress"),
//                (NetworkInterface) ctx.getBean("networkInterface"),
//                (MulticastPublisher) ctx.getBean("publisher"),
//                group);
        MulticastReceiver receiver = ctx.getBean(MulticastReceiver.class);


        receiver.start();

        publisher.sendMessage(MulticastPacketMessage.REPORT);
        group.addAddress(InetAddress.getLocalHost());

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String command = reader.readLine();
                switch (command) {
                    case "exit" -> {
                        MulticastReceiver.continueReading = false;
                        publisher.sendMessage(MulticastPacketMessage.LEAVE);
                        return;
                    }
                    case "show" -> group.show();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}