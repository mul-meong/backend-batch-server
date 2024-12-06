package com.mulmeong.batchserver.contest.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = "com.mulmeong.batchserver.contest.infrastructure.repository",
        mongoTemplateRef = "contestReadMongoTemplate"
)
public class ContestReadDatabaseConfig {

    @Value("${spring.data.mongodb.contest.dbname}")
    private String DB_NAME;

    @Value("${spring.data.mongodb.contest.uri}")
    private String contestMongoUri;

    @Bean(name = "contestReadMongoTemplate")
    public MongoTemplate contestMongoTemplate() {
        MongoClient mongoClient = MongoClients.create(contestMongoUri);
        MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient, DB_NAME);
        return new MongoTemplate(factory);
    }

}
