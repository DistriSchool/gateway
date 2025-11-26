package edu.unifor.br.distrischool.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {

    public AuthorizationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String rolesHeader = request.getHeaders().getFirst("X-User-Roles");

            if (rolesHeader == null || rolesHeader.isEmpty()) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing user roles");
            }

            List<String> userRoles = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            if (config.getRoles() == null || config.getRoles().isEmpty()) {
                return chain.filter(exchange);
            }

            boolean allowed = userRoles.stream().anyMatch(config.getRoles()::contains);

            if (!allowed) {
                return onError(exchange, HttpStatus.FORBIDDEN, "Access denied");
            }

            log.info("User roles {} allowed for required {}", userRoles, config.getRoles());

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        log.error("Authorization error: {}", message);
        return response.setComplete();
    }

    public static class Config {
        private List<String> roles;

        public Config() {
        }

        public Config(List<String> roles) {
            this.roles = roles;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
