package eu.europeana.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Customize global web mvn configuration for application , especially related to CORS
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * The maximum time (in seconds) that the response from a pre-flight request
     * can be cached by the client.
     */
    public static final long MAX_AGE = 1000L;

    /**
     * Setup CORS
     *
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("download.europeana.eu","localhost")
            .allowedMethods("*")
            .allowedHeaders("*")
            .exposedHeaders("Allow","ETag")
            .allowCredentials(false)
            .maxAge(MAX_AGE);
    }

}
