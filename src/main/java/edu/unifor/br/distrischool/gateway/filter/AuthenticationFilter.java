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

            // Lista de endpoints públicos que não precisam de autenticação
            if (isPublicEndpoint(request.getURI().getPath())) {
                return chain.filter(exchange);
            }

            // Verifica se o header Authorization existe
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Token de autorização ausente", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Token inválido", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // Valida o token
                if (!jwtService.validateToken(token)) {
                    return onError(exchange, "Token expirado ou inválido", HttpStatus.UNAUTHORIZED);
                }

                // Extrai informações do token e adiciona aos headers
                String username = jwtService.extractUsername(token);
                List<String> roles = jwtService.extractRoles(token);

                // Adiciona informações do usuário aos headers da requisição
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", username)
                        .header("X-User-Roles", String.join(",", roles))
                        .build();

                log.info("Usuário autenticado: {} com roles: {}", username, roles);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("Erro na validação do token: {}", e.getMessage());
                return onError(exchange, "Falha na autenticação", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isPublicEndpoint(String path) {
        // Endpoints públicos que não precisam de autenticação
        return path.contains("/auth/login") ||
                path.contains("/auth/register") ||
                path.contains("/actuator") ||
                path.contains("/swagger") ||
                path.contains("/api-docs");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error("Erro de autenticação: {}", message);
        return response.setComplete();
    }

    public static class Config {
        // Configurações personalizadas do filtro podem ser adicionadas aqui
    }
}

