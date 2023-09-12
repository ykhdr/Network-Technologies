package ru.ykhdr.selfcopies.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.ykhdr.selfcopies.Group;
import ru.ykhdr.selfcopies.multicast.MulticastPublisher;
import ru.ykhdr.selfcopies.multicast.MulticastReceiver;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

@AllArgsConstructor
@Configuration
@Import(MulticastConfig.class)
@ComponentScan("ru.ykhdr.selfcopies")
public class SpringConfig {
    private MulticastConfig multicastConfig;

    @Bean
    public Group group() {
        return new Group(new ArrayList<>());
    }

//    @Bean
//    public MulticastPublisher publisher() {
//        try {
//            InetAddress group = multicastConfig.groupAddress();
//            int socketPort = multicastConfig.socketPort();
//
//            return new MulticastPublisher(group, socketPort);
//        } catch (UnknownHostException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    @Bean
//    public MulticastReceiver receiver() {
//        try {
//            MulticastSocket multicastSocket = multicastConfig.multicastSocket();
//            InetSocketAddress socketAddress = multicastConfig.socketAddress();
//            NetworkInterface networkInterface = multicastConfig.networkInterface();
//            MulticastPublisher publisher = publisher();
//
//            return new MulticastReceiver(multicastSocket, socketAddress, networkInterface, publisher, group());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//    }
}
