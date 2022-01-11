package com.mami.betterreadsdataloader;

import com.mami.betterreadsdataloader.author.Author;
import com.mami.betterreadsdataloader.author.AuthorRepository;
import com.mami.betterreadsdataloader.connection.DataStaxAstraConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${datadump.location.author}")
  private String authorDumpLocation;

  @Value("${datadump.location.works}")
  private String worksDumpLocation;

  public static void main(String[] args) {
    SpringApplication.run(BetterReadsDataLoaderApplication.class, args);
  }

  private void  initAuthor() {

    Path path = Paths.get(authorDumpLocation);
    try(Stream<String> lines = Files.lines(path)) {

      lines.forEach(line -> {
        // Read and parse the line
        String jsonString = line.substring(line.indexOf("{"));
        try {
          JSONObject jsonObject = new JSONObject(jsonString);

          // Construct Author object
          Author author= new Author();
          author.setName(jsonObject.optString("name"));
          author.setPersonalName(jsonObject.optString("personal_name"));
          author.setId(jsonObject.optString("key").replace("/authors/",""));

          //Persist using Repository
          System.out.println("[INITAUTHOR] ====> saving author : " + author.getName() + " ." );
          authorRepository.save(author);

        } catch (JSONException e) {
          e.printStackTrace();
        }
      });

    } catch (IOException e) {
      e.printStackTrace();
    }

  }
  private void  initWorks() {}

  @PostConstruct
  public void start() {
    System.out.println("[Start]==============================> Application Started! < ==============================");

    initAuthor();
    //initWorks();

  }

  @Bean
  public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraConfig config) {
    Path bundle = config.getSecureConnectBundle().toPath();
    return builder->builder.withCloudSecureConnectBundle(bundle);
  }
}
