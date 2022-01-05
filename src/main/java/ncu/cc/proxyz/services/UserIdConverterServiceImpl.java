package ncu.cc.proxyz.services;

import ncu.cc.proxyz.helpers.SiteSpecials;
import ncu.cc.proxyz.properties.ProxyingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;

@Service
public class UserIdConverterServiceImpl implements UserIdConverterService {
    private static final Logger logger = LoggerFactory.getLogger(UserIdConverterServiceImpl.class);
    private final ProxyingProperties proxyingProperties;

    public UserIdConverterServiceImpl(ProxyingProperties proxyingProperties) {
        this.proxyingProperties = proxyingProperties;
    }

    @Override
    public String convert(Principal p) {
        return Optional.ofNullable(p)
                .map(principal -> Optional.ofNullable(this.proxyingProperties.getSpecialUseridMapping())
                        .filter(Boolean::booleanValue)
                        .map(aBoolean -> principal)
                        .filter(OAuth2AuthenticationToken.class::isInstance)
                        .map(OAuth2AuthenticationToken.class::cast)
                        .map(AbstractAuthenticationToken::getPrincipal)
                        .filter(DefaultOAuth2User.class::isInstance)
                        .map(DefaultOAuth2User.class::cast)
                        .map(DefaultOAuth2User::getAttributes)
                        .map(attributes -> SiteSpecials.attributeToName(principal, attributes))
                        .orElseGet(principal::getName))
                .orElse("");
    }
}
