package ru.ykhdr.selfcopies.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.*;
import java.util.Objects;

@AllArgsConstructor
@Getter
@Configuration
@PropertySource("classpath:multicast.properties")
public class MulticastConfig{
    private final Environment environment;

    @Bean
    public int groupPort() {
        return Integer.parseInt(Objects.requireNonNull(environment.getProperty("multicast.group.port")));
    }

    @Bean
    public int socketPort() {
        return Integer.parseInt(Objects.requireNonNull(environment.getProperty("multicast.socket.port")));
    }

    @Bean
    public InetAddress groupAddress() throws UnknownHostException {
        InetAddress group = InetAddress.getByName(Objects.requireNonNull(environment.getProperty("multicast.group.address")));
        if (!group.isMulticastAddress()) {
            throw new UnknownHostException("Address isn't multicast group address");
        }

        return group;
    }

    @Bean
    public MulticastSocket multicastSocket() throws IOException {
        return new MulticastSocket(socketPort());
    }

    @Bean
    public InetSocketAddress socketAddress() throws UnknownHostException {
        return new InetSocketAddress(groupAddress(),groupPort());
    }
}
