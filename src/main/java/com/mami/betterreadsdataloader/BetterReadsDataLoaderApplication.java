package com.mami.betterreadsdataloader;

import com.mami.betterreadsdataloader.author.Author;
import com.mami.betterreadsdataloader.author.AuthorRepository;
import com.mami.betterreadsdataloader.connection.DataStaxAstraConfig;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraConfig.class)
public class BetterReadsDataLoaderApplication {

  @Autowired
  private AuthorRepository authorRepository;

  public static void main(String[] args) {
    SpringApplication.run(BetterReadsDataLoaderApplication.class, args);
  }

  @PostConstruct
  public void start() {
    System.out.println("[Start]==============================>       Application Started!  < ==============================");
    Author author=new Author();
    author.setId("id");
    author.setName("name");
    author.setPersonalName("personalName");
    authorRepository.save(author);
  }

  @Bean
  public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraConfig config) {
    Path bundle = config.getSecureConnectBundle().toPath();
    return builder->builder.withCloudSecureConnectBundle(bundle);
  }
}
