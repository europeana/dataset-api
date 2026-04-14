package eu.europeana.api.auth;

import eu.europeana.api.commons_sb3.definitions.oauth.Role;
import eu.europeana.api.commons_sb3.oauth2.service.authorization.BaseAuthorizationService;
import eu.europeana.api.config.DatasetServingConfig;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Service;

@Service
public class DatasetAuthService extends BaseAuthorizationService {

    private DatasetServingConfig config;
    public DatasetAuthService(DatasetServingConfig config){
        this.config = config;

    }
    @Override
    protected Role getRoleByName(String s) {
        return null;
    }

    @Override
    protected String getSignatureKey() {
        return config.getJwtTokenSignatureKey();
    }

    @Override
    protected ClientDetailsService getClientDetailsService() {
        return null;
    }
    @Override
    protected String getApiName() {
        return config.getApiNameForAuthorization();
    }

}
