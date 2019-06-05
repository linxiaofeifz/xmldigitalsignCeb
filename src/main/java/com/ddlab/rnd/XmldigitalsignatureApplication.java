package com.ddlab.rnd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

@SpringBootApplication
public class XmldigitalsignatureApplication {

    public static void main(String[] args) {
        SpringApplication.run(XmldigitalsignatureApplication.class, args);
    }

    @Bean
    public ResourceLoader createResourceLoader() {
        return new DefaultResourceLoader();
    }

}
