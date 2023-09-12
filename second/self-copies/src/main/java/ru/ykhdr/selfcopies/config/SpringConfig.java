package ru.ykhdr.selfcopies.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.ykhdr.selfcopies.Group;
import ru.ykhdr.selfcopies.multicast.MulticastPublisher;
import ru.ykhdr.selfcopies.multicast.MulticastReceiver;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

@AllArgsConstructor
@Configuration
@EnableScheduling
@Import(MulticastConfig.class)
@ComponentScan("ru.ykhdr.selfcopies")
public class SpringConfig {
    @Bean
    public Group group() {
        return new Group(new ArrayList<>());
    }
}
