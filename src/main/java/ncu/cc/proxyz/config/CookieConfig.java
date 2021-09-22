package ncu.cc.proxyz.config;

import ncu.cc.proxyz.constants.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@Configuration
public class CookieConfig {
    @Bean
    public WebSessionIdResolver webSessionIdResolver() {

        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName(Constants.SHH_COOKIE_NAME);
        return resolver;
    }
}
