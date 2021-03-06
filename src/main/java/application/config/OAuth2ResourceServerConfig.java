package application.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@EnableWebSecurity
@EnableResourceServer
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {
    private final CloudantProperties cloudantProperties;

    public OAuth2ResourceServerConfig(CloudantProperties cloudantProperties) {
        this.cloudantProperties = cloudantProperties;
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.tokenServices(tokenServices());
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // OAuth protect these endpoints
        http
                .requestMatchers().antMatchers("/customer", "/customer/search").and()
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/customer").access("#oauth2.hasScope('admin')")
                .antMatchers(HttpMethod.POST, "/customer/search").access("#oauth2.hasScope('admin')")
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/customer").access("#oauth2.hasScope('blue')")
                .antMatchers(HttpMethod.PUT, "/customer").access("#oauth2.hasScope('blue')")
                .antMatchers(HttpMethod.DELETE, "/customer").access("#oauth2.hasScope('blue')")
                .and()
                .exceptionHandling().accessDeniedHandler(new OAuth2AccessDeniedHandler());
    }

    @Bean
    @Qualifier("tokenStore")
    protected TokenStore tokenStore() {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    @Bean
    @Qualifier("jwtAccessTokenConverter")
    protected JwtAccessTokenConverter jwtAccessTokenConverter() {
        final JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        /* for HS256, set the signing key */
        converter.setSigningKey(cloudantProperties.getSharedSecret());
        return converter;
    }

    @Bean
    @Primary
    protected DefaultTokenServices tokenServices() {
        final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        System.out.println("tokenServices() " + tokenStore());
        defaultTokenServices.setTokenStore(tokenStore());
        return defaultTokenServices;
    }
}
