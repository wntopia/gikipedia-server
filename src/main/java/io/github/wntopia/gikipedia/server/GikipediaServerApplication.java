package io.github.wntopia.gikipedia.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GikipediaServerApplication {

  static void main(String[] args) {
    SpringApplication.run(GikipediaServerApplication.class, args);
  }
}
