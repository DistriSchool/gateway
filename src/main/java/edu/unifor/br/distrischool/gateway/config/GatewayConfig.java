package edu.unifor.br.distrischool.gateway.config;

import edu.unifor.br.distrischool.gateway.filter.AuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GatewayConfig {
    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Value("${microservices.auth-service.url}")
    private String authServiceUrl;

    @Value("${microservices.student-service.url}")
    private String studentServiceUrl;

    @Bean
    public RouteLocator customRouteLocatorWithoutDiscovery(RouteLocatorBuilder builder) {

        return builder.routes()
                .route("auth-service-me", r -> r
                        .path("/api/auth/me")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                        )
                        .uri(authServiceUrl))
                .route("auth-service-public", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(authServiceUrl))

                .route("student-service", r -> r
                        .path("/api/students/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                        )
                        .uri(studentServiceUrl))

                .build();
    }
}