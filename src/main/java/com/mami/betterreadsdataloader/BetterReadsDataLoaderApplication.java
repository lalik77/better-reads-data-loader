package com.mami.betterreadsdataloader;

import com.mami.betterreadsdataloader.connection.DataStaxAstraConfig;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraConfig.class)
public class BetterReadsDataLoaderApplication {

  public static void main(String[] args) {
    SpringApplication.run(BetterReadsDataLoaderApplication.class, args);
  }


  @Bean
  public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraConfig config) {
    Path bundle = config.getSecureConnectBundle().toPath();
    return builder->builder.withCloudSecureConnectBundle(bundle);
  }

}
