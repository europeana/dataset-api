package eu.europeana.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 *The main entry point for the Dataset Serving Application.
 */
@SpringBootApplication
public class DatasetServingApplication extends SpringBootServletInitializer {

    /**
     * Launches the Dataset Serving application in standalone mode.
     * @param args command-line arguments to be passed to the Spring application context.
     */
    public static void main(String[] args) {
        SpringApplication.run(DatasetServingApplication.class, args);
    }

}
