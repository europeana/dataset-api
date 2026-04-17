package eu.europeana.api.config;

import eu.europeana.api.commons_sb3.auth.AuthenticationBuilder;
import eu.europeana.api.commons_sb3.auth.AuthenticationConfig;
import eu.europeana.api.commons_sb3.error.config.ErrorConfig;
import eu.europeana.api.commons_sb3.error.i18n.I18nService;
import eu.europeana.api.commons_sb3.error.i18n.I18nServiceImpl;
import eu.europeana.api.commons_sb3.oauth2.service.impl.EuropeanaClientDetailsService;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOG = LogManager.getLogger(DatasetServingConfig.class);

    @Value("${keycloak.jwttoken.siganturekey}")
    private String jwtTokenSignatureKey;

    @Value("${dataset.storage.path.local}")
    private String dataSetLocalStoragePath;

    @Value("${api.name.authorization}")
    private String apiNameForAuthorization;

    @Value("${europeana.apikey.serviceurl}")
    private String apiKeyServiceUrl;

    @Value("keycloak.token.endpoint")
    private String tokenEndpoint;

    @Value("keycloak.token.grant.params")
    private String grantParams;

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

    public String getApiKeyServiceUrl() {
        return apiKeyServiceUrl;
    }

    public void setApiKeyServiceUrl(String apiKeyServiceUrl) {
        this.apiKeyServiceUrl = apiKeyServiceUrl;
    }

    @Bean(name = ErrorConfig.BEAN_I18nService)
    public I18nService getI18nService() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(ErrorConfig.COMMON_MESSAGE_SOURCE);
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return new I18nServiceImpl(messageSource);
    }

    @Bean(name = "datasetClientDetailsService")
        public EuropeanaClientDetailsService getClientDetailsService() {
        EuropeanaClientDetailsService clientDetailsService = new EuropeanaClientDetailsService();
        clientDetailsService.setApiKeyServiceUrl(getApiKeyServiceUrl());
        if (StringUtils.isNotEmpty(tokenEndpoint) && StringUtils.isNotEmpty(grantParams)) {
            AuthenticationConfig config = new AuthenticationConfig(tokenEndpoint, grantParams);
            clientDetailsService.setAuthHandler(AuthenticationBuilder.newAuthentication(config));
        } else {
            LOG.error("Keycloak token-endpoint and/or grant-parameters NOT set !! ");
        }
        return clientDetailsService;
    }

}
