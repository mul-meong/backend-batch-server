package com.mulmeong.batchserver.comment.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = "com.mulmeong.batchserver.comment.infrastructure.repository",
        mongoTemplateRef = "commentReadMongoTemplate"
)
@Slf4j
public class CommentMongoConfig {

    @Value("${spring.data.mongodb.comment.database}")
    private String dbName;

    @Value("${spring.data.mongodb.comment.uri}")
    private String commentMongoUri;

    @Bean(name = "commentReadMongoTemplate")
    public MongoTemplate commentMongoTemplate() {
        ConnectionString connectionString = new ConnectionString(commentMongoUri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        MongoClient mongoClient = MongoClients.create(mongoClientSettings);

        return new MongoTemplate(mongoClient, dbName);

    }

}
