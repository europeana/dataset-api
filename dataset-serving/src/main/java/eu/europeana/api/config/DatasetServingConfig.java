package eu.europeana.api.config;

import eu.europeana.api.commons_sb3.error.config.ErrorConfig;
import eu.europeana.api.commons_sb3.error.i18n.I18nService;
import eu.europeana.api.commons_sb3.error.i18n.I18nServiceImpl;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
@PropertySource(
    value = {"classpath:dataset.serving.properties", "classpath:dataset.serving.user.properties"},
    ignoreResourceNotFound = true)
public class DatasetServingConfig {

    @Value("${keycloak.jwttoken.siganturekey}")
    private String jwtTokenSignatureKey;

    @Value("${dataset.storage.path.local}")
    private String dataSetLocalStoragePath;

    @Value("${api.name.authorization}")
    private String apiNameForAuthorization;

    public String getJwtTokenSignatureKey() {
        return jwtTokenSignatureKey;
    }

    public void setJwtTokenSignatureKey(String jwtTokenSignatureKey) {
        this.jwtTokenSignatureKey = jwtTokenSignatureKey;
    }

    public String getDataSetLocalStoragePath() {
        return dataSetLocalStoragePath;
    }

    public void setDataSetLocalStoragePath(String dataSetLocalStoragePath) {
        this.dataSetLocalStoragePath = dataSetLocalStoragePath;
    }

    public String getApiNameForAuthorization() {
        return apiNameForAuthorization;
    }

    public void setApiNameForAuthorization(String apiNameForAuthorization) {
        this.apiNameForAuthorization = apiNameForAuthorization;
    }

    @Bean(name = ErrorConfig.BEAN_I18nService)
    public I18nService getI18nService() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(ErrorConfig.COMMON_MESSAGE_SOURCE);
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return new I18nServiceImpl(messageSource);
    }

}
