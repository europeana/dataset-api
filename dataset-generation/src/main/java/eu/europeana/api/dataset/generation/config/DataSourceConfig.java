package eu.europeana.api.dataset.generation.config;

import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static eu.europeana.api.dataset.generation.utils.AppConfigConstants.BEAN_DATASET_DATA_STORE;

/**
 * DataSourceConfig is a Spring configuration class that sets up MongoDB data source
 * connections and configurations for the application.
 *
 * Components:
 * - MongoClient: Configures and provides a MongoDB client with custom connection
 *   pool settings such as maximum idle time.
 * - Datastore: Configures and provides a Morphia Datastore connected to the
 *   MongoDB client, which is responsible for managing the application-specific
 *   MongoDB database.
 *
 * @author Srishti Singh
 * @since 23 Feb 2026
 */
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties
@PropertySource("classpath:dataset.generation.properties")
@PropertySource(value = "classpath:dataset.generation.user.properties", ignoreResourceNotFound = true)
public class DataSourceConfig {

    private static final Logger LOGGER = LogManager.getLogger(DataSourceConfig.class);

    @Value("${mongo.connectionUrl}")
    private String hostUri;

    @Value("${mongo.max.idle.time.millisec:10000}")
    private long mongoMaxIdleTimeMillisecond;

    @Value("${mongo.dataset.database}")
    private String datasetDatabase;


    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(hostUri);

        // connection pool settings
        Block<ConnectionPoolSettings.Builder> connectionPoolSettingsBlockBuilder =
                (ConnectionPoolSettings.Builder builder) ->
                        builder.maxConnectionIdleTime(mongoMaxIdleTimeMillisecond, TimeUnit.MILLISECONDS);

        return MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(connectionString)
                        .applyToConnectionPoolSettings(connectionPoolSettingsBlockBuilder)
                        .build());
    }

    @Primary
    @Bean(BEAN_DATASET_DATA_STORE)
    public Datastore batchDataStore(MongoClient mongoClient) {
        LOGGER.info("Configuring Dataset database: {}", datasetDatabase);
        Datastore datastore = Morphia.createDatastore(mongoClient, datasetDatabase);
        datastore.ensureIndexes();
        return datastore;
    }
}