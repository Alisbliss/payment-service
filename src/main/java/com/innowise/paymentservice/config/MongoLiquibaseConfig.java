package com.innowise.paymentservice.config;

import jakarta.annotation.PostConstruct;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MongoLiquibaseConfig {

    @Value("${spring.mongodb.uri:mongodb://localhost:27017/paymentdb}")
    private String mongoUri;

    @Value("${spring.liquibase.change-log:classpath:db/changelog/db.changelog-master.xml}")
    private String changeLog;

    @PostConstruct
    public void runMigrations() {
        String resource = changeLog.startsWith("classpath:")
                ? changeLog.substring("classpath:".length())
                : changeLog;
        try {
            MongoLiquibaseDatabase database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                    .openDatabase(mongoUri, null, null, null, null);
            Liquibase liquibase = new Liquibase(resource, new ClassLoaderResourceAccessor(), database);
            try {
                liquibase.update(new Contexts(), new LabelExpression());
                log.info("MongoDB Liquibase migrations applied from {}", resource);
            } finally {
                liquibase.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run MongoDB Liquibase migrations", e);
        }
    }
}