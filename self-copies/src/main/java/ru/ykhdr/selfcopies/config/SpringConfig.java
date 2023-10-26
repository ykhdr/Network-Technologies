package ru.ykhdr.selfcopies.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@AllArgsConstructor
@Configuration
@EnableScheduling
@Import(MulticastConfig.class)
@ComponentScan("ru.ykhdr.selfcopies")
public class SpringConfig {
}
