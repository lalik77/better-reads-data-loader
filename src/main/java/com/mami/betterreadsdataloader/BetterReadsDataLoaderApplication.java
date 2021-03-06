package com.mami.betterreadsdataloader;

import com.mami.betterreadsdataloader.author.Author;
import com.mami.betterreadsdataloader.author.AuthorRepository;
import com.mami.betterreadsdataloader.book.Book;
import com.mami.betterreadsdataloader.book.BookRepository;
import com.mami.betterreadsdataloader.connection.DataStaxAstraConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.json.JSONArray;
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

  @Autowired
  private BookRepository bookRepository;

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

          final String result1 = jsonObject.optString("key").replace("/a/", "");
          if(!result1.startsWith("/authors/")) {
          author.setId(result1);
          } else {
            final String result2 = jsonObject.optString("key").replace("/authors/", "");
            author.setId(result2);
          }

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

  private void initWorks() {
    Path path = Paths.get(worksDumpLocation);
    DateTimeFormatter dateFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    try (Stream<String> lines = Files.lines(path)) {

      lines.forEach(line -> {

        //Read and parse
        String jsonString = line.substring(line.indexOf("{"));
        try {
          JSONObject jsonObject = new JSONObject(jsonString);

          //Construct Book Object
          Book book = new Book();
          book.setId(jsonObject.getString("key").replace("/works/",""));
          book.setName(jsonObject.optString("title"));

          JSONObject descriptionObj = jsonObject.optJSONObject("description");
          if (descriptionObj != null) {
            book.setDescription(descriptionObj.optString("value"));
          }

          JSONObject publishedObject = jsonObject.optJSONObject("created");
          if (publishedObject != null) {
            String dateString = publishedObject.getString("value");
            book.setPublishedDate(LocalDate.parse(dateString,dateFormatter));
          }

          JSONArray coversJSONArr = jsonObject.optJSONArray("covers");
          if (coversJSONArr != null) {
            List<String> coverIds = new ArrayList<>();
            for (int i = 0; i < coversJSONArr.length(); i++) {
              coverIds.add(coversJSONArr.getString(i));
            }
            book.setCoverIds(coverIds);
          }

          JSONArray authorsJSONArr = jsonObject.optJSONArray("authors");
          if (authorsJSONArr != null) {

            List<String> authorIds = new ArrayList<>();

            for (int i = 0; i < authorsJSONArr.length(); i++) {

              /*String authorId = authorsJSONArr.getJSONObject(i)
                  .getJSONObject("author")
                  .getString("key").replace("/a/", "");*/
              String authorId=null;

              String result1 = authorsJSONArr.getJSONObject(i)
                  .getJSONObject("author")
                  .getString("key").replace("/a/", "");

              if(!result1.startsWith("/authors/")) {

                authorId = result1;

              } else {
                authorId = authorsJSONArr.getJSONObject(i)
                    .getJSONObject("author")
                    .getString("key").replace("/authors/", "");
              }



              authorIds.add(authorId);
            }
            book.setAuthorIds(authorIds);

            List<String> authorNames = authorIds.stream()
                .map(id -> authorRepository.findById(id))
                .map(optionalAuthor -> {
                  if (!optionalAuthor.isPresent()) {
                    return "Unknown Author";
                  }
                  return optionalAuthor.get().getName();
                })
                .collect(Collectors.toList());

            book.setAuthorNames(authorNames);

            //Persist using Repository
            System.out.println("[INITWORKS] ====> saving book : " + book.getName() + " ." );
            bookRepository.save(book);
          }


        } catch (JSONException e) {
          e.printStackTrace();
        }

      });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @PostConstruct
  public void start() {
    System.out.println(
        "[Start]==============================> Application Started! < ==============================");

   initAuthor();
   initWorks();

  }

  @Bean
  public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraConfig config) {
    Path bundle = config.getSecureConnectBundle().toPath();
    return builder->builder.withCloudSecureConnectBundle(bundle);
  }
}
