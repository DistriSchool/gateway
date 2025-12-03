package edu.unifor.br.distrischool.gateway.config;

import edu.unifor.br.distrischool.gateway.filter.AuthenticationFilter;
import edu.unifor.br.distrischool.gateway.filter.AuthorizationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.List;

@Slf4j
@Configuration
public class GatewayConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Autowired
    private AuthorizationFilter authorizationFilter;

    @Value("${microservices.auth-service.url}")
    private String authServiceUrl;

    @Value("${microservices.student-service.url}")
    private String studentServiceUrl;

    @Value("${microservices.teacher-service.url}")
    private String teacherServiceUrl;

    @Value("${microservices.classroom-service.url}")
    private String classroomServiceUrl;

    @Value("${microservices.course-service.url}")
    private String courseServiceUrl;

    @Bean
    public RouteLocator customRouteLocatorWithoutDiscovery(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service-me", r -> r
                        .path("/api/auth/me")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .circuitBreaker(config -> config
                                        .setName("authService")
                                        .setFallbackUri("forward:/fallback/auth")
                                        .setStatusCodes(Set.of("500", "502", "503", "504"))))
                        .uri(authServiceUrl))

                .route("auth-service-public", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(config -> config
                                        .setName("authService")
                                        .setFallbackUri("forward:/fallback/auth")
                                        .setStatusCodes(Set.of("500", "502", "503", "504"))))
                        .uri(authServiceUrl))

                .route("student-service", r -> r
                        .path("/api/students/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(new AuthorizationFilter.Config(List.of("ADMIN"))))
                                .circuitBreaker(config -> config
                                        .setName("studentService")
                                        .setFallbackUri("forward:/fallback/students")
                                        .setStatusCodes(Set.of("500", "502", "503", "504"))))
                        .uri(studentServiceUrl))

                .route("teacher-service", r -> r
                        .path("/api/teachers/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(new AuthorizationFilter.Config(List.of("ADMIN")))))
                        .uri(teacherServiceUrl))

                .route("classroom-service", r -> r
                        .path("/api/classrooms/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(new AuthorizationFilter.Config(List.of("ADMIN")))))
                        .uri(classroomServiceUrl))

                .route("discipline-service", r -> r
                        .path("/api/disciplines/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(new AuthorizationFilter.Config(List.of("ADMIN")))))
                        .uri(classroomServiceUrl))

                .route("course-service", r -> r
                        .path("/api/courses/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(authorizationFilter.apply(new AuthorizationFilter.Config(List.of("ADMIN")))))
                        .uri(courseServiceUrl))
                .build();
    }
}
