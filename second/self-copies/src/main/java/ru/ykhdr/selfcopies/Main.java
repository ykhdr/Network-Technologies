package ru.ykhdr.selfcopies;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.ykhdr.selfcopies.config.SpringConfig;
import ru.ykhdr.selfcopies.multicast.MulticastPacketMessage;
import ru.ykhdr.selfcopies.multicast.MulticastPublisher;
import ru.ykhdr.selfcopies.multicast.MulticastReceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Main {
    public static void main(String[] args){
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfig.class);

        Group group = ctx.getBean(Group.class);

        MulticastPublisher publisher = ctx.getBean(MulticastPublisher.class);
        MulticastReceiver receiver = ctx.getBean(MulticastReceiver.class);

        receiver.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String command = reader.readLine();
                switch (command) {
                    case "exit" -> {
                        MulticastReceiver.setContinueReading(false);
                        publisher.sendMessage(MulticastPacketMessage.LEAVE);
                        publisher.closeSocket();
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