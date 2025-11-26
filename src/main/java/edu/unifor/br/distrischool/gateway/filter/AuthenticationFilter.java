package edu.unifor.br.distrischool.gateway.filter;

import edu.unifor.br.distrischool.gateway.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtService jwtService;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (isPublicEndpoint(request.getURI().getPath())) {
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Token de autorização ausente");
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Token inválido");
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtService.validateToken(token)) {
                    return onError(exchange, "Token expirado ou inválido");
                }

                String username = jwtService.extractUsername(token);
                List<String> roles = jwtService.extractRoles(token);

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", username)
                        .header("X-User-Roles", String.join(",", roles))
                        .build();

                log.info("Usuário autenticado: {} com roles: {}", username, roles);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("Erro na validação do token: {}", e.getMessage());
                return onError(exchange, "Falha na autenticação");
            }
        };
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/auth/login") ||
                path.contains("/actuator") ||
                path.contains("/swagger") ||
                path.contains("/api-docs");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        log.error("Erro de autenticação: {}", message);
        return response.setComplete();
    }

    public static class Config {
    }
}

