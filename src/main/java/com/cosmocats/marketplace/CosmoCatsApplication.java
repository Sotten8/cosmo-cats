package com.cosmocats.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CosmoCatsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CosmoCatsApplication.class, args);
    }
}
