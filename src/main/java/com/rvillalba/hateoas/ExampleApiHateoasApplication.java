package com.rvillalba.hateoas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExampleApiHateoasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApiHateoasApplication.class, args);
    }
}
